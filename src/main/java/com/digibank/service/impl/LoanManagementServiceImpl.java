package com.digibank.service.impl;

import com.digibank.dto.loan.LoanAccountOption;
import com.digibank.dto.loan.LoanApplicationFormView;
import com.digibank.dto.loan.LoanApplicationRequest;
import com.digibank.dto.loan.LoanApplicationView;
import com.digibank.dto.loan.LoanRepaymentRequest;
import com.digibank.dto.loan.LoanScheduleView;
import com.digibank.entity.AccountTransaction;
import com.digibank.entity.AuditLog;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Customer;
import com.digibank.entity.CustomerNotification;
import com.digibank.entity.LoanApplication;
import com.digibank.entity.LoanRepaymentSchedule;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountTransactionType;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.LoanStatus;
import com.digibank.enums.NotificationType;
import com.digibank.enums.TransactionDirection;
import com.digibank.exception.LoanException;
import com.digibank.repository.AccountTransactionRepository;
import com.digibank.repository.AuditLogRepository;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerNotificationRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.repository.LoanApplicationRepository;
import com.digibank.repository.LoanRepaymentScheduleRepository;
import com.digibank.service.LoanManagementService;
import com.digibank.util.SensitiveDataMasker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LoanManagementServiceImpl implements LoanManagementService {

	private static final BigDecimal MIN_AMOUNT = new BigDecimal("50000.00");
	private static final BigDecimal MAX_AMOUNT = new BigDecimal("5000000.00");
	private static final BigDecimal MAX_INCOME_RATIO = new BigDecimal("0.40");

	private final CustomerRepository customerRepository;
	private final BankAccountRepository accountRepository;
	private final LoanApplicationRepository loanRepository;
	private final LoanRepaymentScheduleRepository scheduleRepository;
	private final AccountTransactionRepository transactionRepository;
	private final CustomerNotificationRepository notificationRepository;
	private final AuditLogRepository auditLogRepository;
	private final PasswordEncoder passwordEncoder;
	private final SensitiveDataMasker dataMasker;

	public LoanManagementServiceImpl(CustomerRepository customerRepository, BankAccountRepository accountRepository,
			LoanApplicationRepository loanRepository, LoanRepaymentScheduleRepository scheduleRepository,
			AccountTransactionRepository transactionRepository,
			CustomerNotificationRepository notificationRepository, AuditLogRepository auditLogRepository,
			PasswordEncoder passwordEncoder, SensitiveDataMasker dataMasker) {
		this.customerRepository = customerRepository;
		this.accountRepository = accountRepository;
		this.loanRepository = loanRepository;
		this.scheduleRepository = scheduleRepository;
		this.transactionRepository = transactionRepository;
		this.notificationRepository = notificationRepository;
		this.auditLogRepository = auditLogRepository;
		this.passwordEncoder = passwordEncoder;
		this.dataMasker = dataMasker;
	}

	@Override
	public LoanApplicationFormView getApplicationForm(Long userId) {
		Customer customer = customer(userId);
		return new LoanApplicationFormView(eligibleAccounts(customer).stream()
				.map(account -> new LoanAccountOption(account.getAccountNumber(), account.getAccountType().name(),
						account.getAvailableBalance()))
				.toList(), com.digibank.enums.LoanType.values());
	}

	@Override
	@Transactional
	public LoanApplicationView apply(Long userId, String actorUsername, LoanApplicationRequest request) {
		Customer customer = customer(userId);
		if (customer.getStatus() != CustomerStatus.ACTIVE) {
			throw new LoanException("Only active customers can apply for a loan.");
		}
		if (loanRepository.existsByCustomerIdAndStatus(customer.getId(), LoanStatus.PENDING_REVIEW)) {
			throw new LoanException("You already have a loan application awaiting review.");
		}
		validateRequest(request);
		BankAccount account = eligibleAccounts(customer).stream()
				.filter(candidate -> candidate.getAccountNumber().equals(request.getAccountNumber()))
				.findFirst().orElseThrow(() -> new LoanException("Select an active LKR account owned by you."));
		BigDecimal rate = request.getLoanType().getAnnualInterestRate();
		BigDecimal installment = monthlyPayment(request.getRequestedAmount(), rate, request.getTermMonths());
		validateAffordability(installment, request.getMonthlyIncome());
		LoanApplication loan = new LoanApplication(customer, account, applicationNumber(), request.getLoanType(),
				money(request.getRequestedAmount()), rate, request.getTermMonths(), installment,
				money(request.getMonthlyIncome()), bounded(request.getEmploymentStatus(), 80, "Employment status"),
				bounded(request.getPurpose(), 500, "Loan purpose"));
		loanRepository.save(loan);
		auditLogRepository.save(new AuditLog(actor(actorUsername), "LOAN_APPLICATION_SUBMITTED", "LOAN_APPLICATION",
				loan.getApplicationNumber(), null, LoanStatus.PENDING_REVIEW.name(),
				"Customer submitted a " + loan.getLoanType().name() + " loan application.", LocalDateTime.now()));
		return view(loan, List.of());
	}

	@Override
	@Transactional
	public List<LoanApplicationView> getCustomerLoans(Long userId) {
		customer(userId);
		refreshOverdue();
		return loanRepository.findByCustomerUserIdOrderByCreatedAtDesc(userId).stream()
				.map(loan -> view(loan, schedule(loan))).toList();
	}

	@Override
	@Transactional
	public LoanApplicationView getCustomerLoan(Long userId, String applicationNumber) {
		refreshOverdue();
		LoanApplication loan = loanRepository.findByApplicationNumberAndCustomerUserId(applicationNumber, userId)
				.orElseThrow(() -> new LoanException("Loan application was not found."));
		return view(loan, schedule(loan));
	}

	@Override
	@Transactional
	public List<LoanApplicationView> getLoansForReview(LoanStatus status) {
		refreshOverdue();
		LoanStatus selected = status == null ? LoanStatus.PENDING_REVIEW : status;
		return loanRepository.findByStatusOrderByCreatedAtAsc(selected).stream()
				.map(loan -> view(loan, schedule(loan))).toList();
	}

	@Override
	@Transactional
	public LoanApplicationView getLoanForStaff(String applicationNumber) {
		refreshOverdue();
		LoanApplication loan = loanRepository.findByApplicationNumber(applicationNumber)
				.orElseThrow(() -> new LoanException("Loan application was not found."));
		return view(loan, schedule(loan));
	}

	@Override
	@Transactional
	public void approveAndDisburse(String actorUsername, String applicationNumber, BigDecimal approvedAmount,
			String note) {
		LoanApplication loan = loanRepository.findByApplicationNumberForUpdate(applicationNumber)
				.orElseThrow(() -> new LoanException("Loan application was not found."));
		requirePending(loan);
		BigDecimal amount = money(approvedAmount == null ? loan.getRequestedAmount() : approvedAmount);
		if (amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(loan.getRequestedAmount()) > 0) {
			throw new LoanException("Approved amount must be at least LKR 50,000 and cannot exceed the requested amount.");
		}
		BigDecimal installment = monthlyPayment(amount, loan.getAnnualInterestRate(), loan.getTermMonths());
		validateAffordability(installment, loan.getMonthlyIncome());
		BankAccount account = accountRepository
				.findAllByAccountNumberInForUpdate(List.of(loan.getDisbursementAccount().getAccountNumber())).stream()
				.findFirst().orElseThrow(() -> new LoanException("Disbursement account was not found."));
		if (account.getAccountStatus() != AccountStatus.ACTIVE || account.getCurrencyCode() != CurrencyCode.LKR) {
			throw new LoanException("The customer's disbursement account is not active.");
		}
		LocalDateTime now = LocalDateTime.now();
		account.setAvailableBalance(account.getAvailableBalance().add(amount));
		account.setCurrentBalance(account.getCurrentBalance().add(amount));
		accountRepository.save(account);
		loan.disburse(actor(actorUsername), optional(note, 500), amount, installment, now);
		loanRepository.saveAndFlush(loan);
		transactionRepository.save(new AccountTransaction(account, loan.getApplicationNumber(),
				TransactionDirection.CREDIT, AccountTransactionType.LOAN_DISBURSEMENT, amount,
				account.getAvailableBalance(), "DigiBank Loan Department", "DIGIBANK", "Loan disbursement", now));
		scheduleRepository.saveAll(buildSchedule(loan, amount, installment, now.toLocalDate()));
		String message = String.format(Locale.ROOT,
				"Your %s loan of LKR %s was approved and credited to account %s.",
				loan.getLoanType().getDisplayName(), amount.toPlainString(),
				dataMasker.maskAccountNumber(account.getAccountNumber()));
		notificationRepository.save(new CustomerNotification(loan.getCustomer().getUser(),
				NotificationType.ACCOUNT_NOTICE, "Loan approved and disbursed", message, loan.getApplicationNumber()));
		auditLogRepository.save(new AuditLog(actor(actorUsername), "LOAN_APPROVED_AND_DISBURSED", "LOAN_APPLICATION",
				loan.getApplicationNumber(), LoanStatus.PENDING_REVIEW.name(), LoanStatus.DISBURSED.name(),
				"Loan approved and credited to the customer's account.", now));
	}

	@Override
	@Transactional
	public void reject(String actorUsername, String applicationNumber, String reason) {
		LoanApplication loan = loanRepository.findByApplicationNumberForUpdate(applicationNumber)
				.orElseThrow(() -> new LoanException("Loan application was not found."));
		requirePending(loan);
		String cleanReason = clean(reason);
		if (cleanReason == null || cleanReason.length() < 5) {
			throw new LoanException("Enter a rejection reason of at least 5 characters.");
		}
		if (cleanReason.length() > 200) {
			throw new LoanException("Rejection reason cannot exceed 200 characters.");
		}
		LocalDateTime now = LocalDateTime.now();
		loan.reject(actor(actorUsername), cleanReason, now);
		loanRepository.save(loan);
		notificationRepository.save(new CustomerNotification(loan.getCustomer().getUser(),
				NotificationType.ACCOUNT_NOTICE, "Loan application update",
				"Your loan application " + loan.getApplicationNumber() + " was not approved. Reason: " + cleanReason,
				loan.getApplicationNumber()));
		auditLogRepository.save(new AuditLog(actor(actorUsername), "LOAN_APPLICATION_REJECTED", "LOAN_APPLICATION",
				loan.getApplicationNumber(), LoanStatus.PENDING_REVIEW.name(), LoanStatus.REJECTED.name(),
				cleanReason, now));
	}

	@Override
	public List<LoanAccountOption> getRepaymentAccounts(Long userId) {
		Customer customer = customer(userId);
		return eligibleAccounts(customer).stream()
				.map(account -> new LoanAccountOption(account.getAccountNumber(), account.getAccountType().name(),
						account.getAvailableBalance()))
				.toList();
	}

	@Override
	@Transactional
	public String payInstallment(Long userId, String actorUsername, String applicationNumber, int installmentNumber,
			LoanRepaymentRequest request) {
		Customer customer = customer(userId);
		if (request == null || clean(request.getAccountNumber()) == null) {
			throw new LoanException("Select an account to debit.");
		}
		requireTransactionPin(customer, request.getTransactionPin());
		refreshOverdue();
		LoanApplication loan = loanRepository.findByApplicationNumberForUpdate(applicationNumber)
				.orElseThrow(() -> new LoanException("Loan application was not found."));
		if (!loan.getCustomer().getId().equals(customer.getId())) {
			throw new LoanException("Loan application was not found.");
		}
		if (loan.getStatus() != LoanStatus.DISBURSED) {
			throw new LoanException("Only an active disbursed loan can receive repayments.");
		}
		List<com.digibank.enums.RepaymentStatus> unpaidStatuses = List.of(
				com.digibank.enums.RepaymentStatus.SCHEDULED, com.digibank.enums.RepaymentStatus.OVERDUE);
		LoanRepaymentSchedule next = scheduleRepository.findUnpaid(loan.getId(), unpaidStatuses).stream()
				.findFirst().orElseThrow(() -> new LoanException("This loan has no unpaid installments."));
		if (next.getInstallmentNumber() != installmentNumber) {
			throw new LoanException("Installments must be paid in due-date order.");
		}
		LoanRepaymentSchedule installment = scheduleRepository
				.findInstallmentForUpdate(loan.getId(), installmentNumber)
				.orElseThrow(() -> new LoanException("Loan installment was not found."));
		if (installment.getStatus() == com.digibank.enums.RepaymentStatus.PAID) {
			throw new LoanException("This installment has already been paid.");
		}
		BankAccount account = accountRepository
				.findAllByAccountNumberInForUpdate(List.of(clean(request.getAccountNumber()))).stream()
				.findFirst().orElseThrow(() -> new LoanException("The selected debit account was not found."));
		requireRepaymentAccount(customer, account, installment.getTotalAmount());
		BigDecimal amount = installment.getTotalAmount();
		account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
		account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
		accountRepository.save(account);
		String reference = repaymentReference();
		LocalDateTime now = LocalDateTime.now();
		installment.markPaid(reference, account, account.getAvailableBalance(), now);
		scheduleRepository.saveAndFlush(installment);
		transactionRepository.save(new AccountTransaction(account, reference, TransactionDirection.DEBIT,
				AccountTransactionType.LOAN_REPAYMENT, amount, account.getAvailableBalance(),
				"DigiBank Loan Department", loan.getApplicationNumber(),
				"Loan installment " + installmentNumber, now));
		if (scheduleRepository.countByLoanApplicationIdAndStatusNot(loan.getId(),
				com.digibank.enums.RepaymentStatus.PAID) == 0) {
			loan.close();
			loanRepository.save(loan);
		}
		String message = String.format(Locale.ROOT,
				"LKR %s paid for installment %d of loan %s. Reference: %s.", amount.toPlainString(),
				installmentNumber, loan.getApplicationNumber(), reference);
		notificationRepository.save(new CustomerNotification(customer.getUser(), NotificationType.ACCOUNT_NOTICE,
				"Loan installment paid", message, reference));
		auditLogRepository.save(new AuditLog(actor(actorUsername), "LOAN_INSTALLMENT_PAID", "LOAN_APPLICATION",
				loan.getApplicationNumber(), LoanStatus.DISBURSED.name(), loan.getStatus().name(),
				"Installment " + installmentNumber + " paid under reference " + reference + ".", now));
		return reference;
	}

	private List<LoanRepaymentSchedule> buildSchedule(LoanApplication loan, BigDecimal principal,
			BigDecimal installment, LocalDate disbursedDate) {
		List<LoanRepaymentSchedule> rows = new ArrayList<>();
		BigDecimal balance = principal;
		BigDecimal monthlyRate = loan.getAnnualInterestRate().divide(new BigDecimal("1200"), 12,
				RoundingMode.HALF_UP);
		for (int number = 1; number <= loan.getTermMonths(); number++) {
			BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
			BigDecimal principalPart = installment.subtract(interest).setScale(2, RoundingMode.HALF_UP);
			if (number == loan.getTermMonths() || principalPart.compareTo(balance) > 0) {
				principalPart = balance;
			}
			BigDecimal total = principalPart.add(interest).setScale(2, RoundingMode.HALF_UP);
			rows.add(new LoanRepaymentSchedule(loan, number, disbursedDate.plusMonths(number), principalPart,
					interest, total));
			balance = balance.subtract(principalPart).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
		}
		return rows;
	}

	private BigDecimal monthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
		double monthlyRate = annualRate.doubleValue() / 1200.0;
		double factor = Math.pow(1.0 + monthlyRate, months);
		double payment = principal.doubleValue() * monthlyRate * factor / (factor - 1.0);
		return BigDecimal.valueOf(payment).setScale(2, RoundingMode.HALF_UP);
	}

	private void validateRequest(LoanApplicationRequest request) {
		if (request == null || request.getLoanType() == null || request.getRequestedAmount() == null
				|| request.getMonthlyIncome() == null) {
			throw new LoanException("Complete all required loan application fields.");
		}
		BigDecimal amount = request.getRequestedAmount();
		if (amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
			throw new LoanException("Loan amount must be between LKR 50,000 and LKR 5,000,000.");
		}
		if (request.getTermMonths() < 6 || request.getTermMonths() > 60) {
			throw new LoanException("Loan term must be between 6 and 60 months.");
		}
		if (clean(request.getEmploymentStatus()) == null || clean(request.getPurpose()) == null) {
			throw new LoanException("Employment status and loan purpose are required.");
		}
	}

	private void validateAffordability(BigDecimal installment, BigDecimal monthlyIncome) {
		if (monthlyIncome == null || monthlyIncome.signum() <= 0
				|| installment.compareTo(monthlyIncome.multiply(MAX_INCOME_RATIO)) > 0) {
			throw new LoanException("Estimated monthly installment cannot exceed 40% of the declared monthly income.");
		}
	}

	private List<BankAccount> eligibleAccounts(Customer customer) {
		return accountRepository.findByCustomerId(customer.getId()).stream()
				.filter(account -> account.getAccountStatus() == AccountStatus.ACTIVE)
				.filter(account -> account.getCurrencyCode() == CurrencyCode.LKR)
				.toList();
	}

	private Customer customer(Long userId) {
		return customerRepository.findByUserId(userId)
				.orElseThrow(() -> new LoanException("Customer profile was not found."));
	}

	private void requirePending(LoanApplication loan) {
		if (loan.getStatus() != LoanStatus.PENDING_REVIEW) {
			throw new LoanException("Only pending loan applications can be reviewed.");
		}
	}

	private List<LoanScheduleView> schedule(LoanApplication loan) {
		if (loan.getId() == null) {
			return List.of();
		}
		List<LoanRepaymentSchedule> rows = scheduleRepository
				.findByLoanApplicationIdOrderByInstallmentNumber(loan.getId());
		int nextUnpaid = rows.stream()
				.filter(row -> row.getStatus() != com.digibank.enums.RepaymentStatus.PAID)
				.mapToInt(LoanRepaymentSchedule::getInstallmentNumber).min().orElse(-1);
		return rows.stream().map(row -> new LoanScheduleView(row.getInstallmentNumber(), row.getDueDate(),
				row.getPrincipalAmount(), row.getInterestAmount(), row.getTotalAmount(), row.getStatus(),
				row.getInstallmentNumber() == nextUnpaid && loan.getStatus() == LoanStatus.DISBURSED,
				row.getPaymentReference(), row.getPaidAt())).toList();
	}

	private LoanApplicationView view(LoanApplication loan, List<LoanScheduleView> schedule) {
		Customer customer = loan.getCustomer();
		return new LoanApplicationView(loan.getApplicationNumber(), customer.getCustomerNumber(), customer.getFullName(),
				dataMasker.maskAccountNumber(loan.getDisbursementAccount().getAccountNumber()), loan.getLoanType(),
				loan.getStatus(), loan.getRequestedAmount(), loan.getApprovedAmount(), loan.getAnnualInterestRate(),
				loan.getTermMonths(), loan.getMonthlyInstallment(), loan.getMonthlyIncome(), loan.getEmploymentStatus(),
				loan.getPurpose(), loan.getReviewedBy(), loan.getReviewedAt(), loan.getReviewNote(), loan.getDisbursedAt(),
				loan.getCreatedAt(), schedule);
	}

	private String applicationNumber() {
		for (int attempt = 0; attempt < 5; attempt++) {
			String value = "LON" + UUID.randomUUID().toString().replace("-", "").substring(0, 17)
					.toUpperCase(Locale.ROOT);
			if (!loanRepository.existsByApplicationNumber(value)) {
				return value;
			}
		}
		throw new LoanException("Could not generate a loan application number. Try again.");
	}

	private String repaymentReference() {
		for (int attempt = 0; attempt < 5; attempt++) {
			String value = "LRP" + UUID.randomUUID().toString().replace("-", "").substring(0, 17)
					.toUpperCase(Locale.ROOT);
			if (!scheduleRepository.existsByPaymentReference(value)) {
				return value;
			}
		}
		throw new LoanException("Could not generate a repayment reference. Try again.");
	}

	private void requireTransactionPin(Customer customer, String value) {
		String pin = clean(value);
		String hash = customer.getUser().getTransactionPinHash();
		if (pin == null || !pin.matches("^\\d{4}$") || hash == null || !passwordEncoder.matches(pin, hash)) {
			throw new LoanException("Transaction PIN is incorrect.");
		}
	}

	private void requireRepaymentAccount(Customer customer, BankAccount account, BigDecimal amount) {
		if (account.getCustomer() == null || !account.getCustomer().getId().equals(customer.getId())) {
			throw new LoanException("The selected debit account is not owned by you.");
		}
		if (account.getAccountStatus() != AccountStatus.ACTIVE || account.getCurrencyCode() != CurrencyCode.LKR) {
			throw new LoanException("The selected debit account must be an active LKR account.");
		}
		if (account.getAvailableBalance().compareTo(amount) < 0 || account.getCurrentBalance().compareTo(amount) < 0) {
			throw new LoanException("Insufficient account balance for this installment.");
		}
	}

	private void refreshOverdue() {
		scheduleRepository.markOverdue(LocalDate.now());
	}

	private BigDecimal money(BigDecimal value) {
		if (value == null) {
			throw new LoanException("A monetary amount is required.");
		}
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private String clean(String value) {
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}

	private String optional(String value, int maxLength) {
		String cleaned = clean(value);
		if (cleaned == null) {
			return "Approved after staff review.";
		}
		if (cleaned.length() > maxLength) {
			throw new LoanException("Review note cannot exceed " + maxLength + " characters.");
		}
		return cleaned;
	}

	private String bounded(String value, int maxLength, String label) {
		String cleaned = clean(value);
		if (cleaned == null) {
			throw new LoanException(label + " is required.");
		}
		if (cleaned.length() > maxLength) {
			throw new LoanException(label + " cannot exceed " + maxLength + " characters.");
		}
		return cleaned;
	}

	private String actor(String value) {
		String cleaned = clean(value);
		return cleaned == null ? "SYSTEM" : cleaned;
	}
}
