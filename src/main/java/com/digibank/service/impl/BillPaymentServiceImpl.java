package com.digibank.service.impl;

import com.digibank.dto.bill.*;
import com.digibank.entity.*;
import com.digibank.enums.*;
import com.digibank.exception.BillPaymentException;
import com.digibank.exception.CustomerProfileNotFoundException;
import com.digibank.repository.*;
import com.digibank.service.BillPaymentService;
import com.digibank.util.SensitiveDataMasker;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class BillPaymentServiceImpl implements BillPaymentService {
	private static final BigDecimal MIN_AMOUNT = new BigDecimal("10.00");
	private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");

	private final CustomerRepository customerRepository;
	private final BankAccountRepository bankAccountRepository;
	private final SavedBillerRepository savedBillerRepository;
	private final BillPaymentRepository billPaymentRepository;
	private final AccountTransactionRepository transactionRepository;
	private final CustomerNotificationRepository notificationRepository;
	private final AuditLogRepository auditLogRepository;
	private final PasswordEncoder passwordEncoder;
	private final SensitiveDataMasker masker;

	public BillPaymentServiceImpl(CustomerRepository customerRepository, BankAccountRepository bankAccountRepository,
			SavedBillerRepository savedBillerRepository, BillPaymentRepository billPaymentRepository,
			AccountTransactionRepository transactionRepository, CustomerNotificationRepository notificationRepository,
			AuditLogRepository auditLogRepository, PasswordEncoder passwordEncoder, SensitiveDataMasker masker) {
		this.customerRepository = customerRepository;
		this.bankAccountRepository = bankAccountRepository;
		this.savedBillerRepository = savedBillerRepository;
		this.billPaymentRepository = billPaymentRepository;
		this.transactionRepository = transactionRepository;
		this.notificationRepository = notificationRepository;
		this.auditLogRepository = auditLogRepository;
		this.passwordEncoder = passwordEncoder;
		this.masker = masker;
	}

	@Override
	@Transactional(readOnly = true)
	public BillPaymentFormView getPaymentForm(Long userId) {
		Customer customer = customer(userId);
		List<BillAccountOption> accounts = bankAccountRepository.findByCustomerId(customer.getId()).stream()
				.filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE && a.getCurrencyCode() == CurrencyCode.LKR)
				.sorted(Comparator.comparing(BankAccount::getAccountNumber))
				.map(a -> new BillAccountOption(a.getAccountNumber(), masker.maskAccountNumber(a.getAccountNumber()),
						a.getAccountType().name(), a.getAvailableBalance())).toList();
		List<BillerProviderOption> providers = Arrays.stream(BillerProvider.values())
				.map(p -> new BillerProviderOption(p, p.getCategory(), p.getDisplayName(), p.getReferenceLabel())).toList();
		return new BillPaymentFormView(accounts, savedBillers(customer), providers, BillerCategory.values());
	}

	@Override
	@Transactional
	public BillPaymentDetailsView pay(Long userId, String actorUsername, BillPaymentRequest request) {
		Customer customer = customer(userId);
		requireActive(customer);
		if (request == null || request.getSelectionType() == null || trim(request.getSourceAccountNumber()) == null) {
			throw new BillPaymentException("Select a source account and biller.");
		}
		BigDecimal amount = amount(request.getAmount());
		requirePin(customer, request.getTransactionPin());

		SavedBiller savedBiller = null;
		BillerProvider provider;
		String consumerReference;
		if (request.getSelectionType() == BillerSelectionType.SAVED_BILLER) {
			if (request.getSavedBillerId() == null) throw new BillPaymentException("Select a saved biller.");
			savedBiller = savedBillerRepository.findByIdAndCustomerIdAndStatus(request.getSavedBillerId(), customer.getId(),
					SavedBillerStatus.ACTIVE).orElseThrow(() -> new BillPaymentException("The selected saved biller is unavailable."));
			provider = savedBiller.getProvider();
			consumerReference = savedBiller.getConsumerReference();
		} else {
			provider = request.getProvider();
			if (provider == null) throw new BillPaymentException("Select a service provider.");
			consumerReference = normalizeReference(request.getConsumerReference());
			if (request.isSaveBiller()) {
				savedBiller = createSavedBiller(customer, provider, request.getNickname(), consumerReference);
			}
		}

		String accountNumber = trim(request.getSourceAccountNumber());
		BankAccount source = bankAccountRepository.findAllByAccountNumberInForUpdate(List.of(accountNumber)).stream()
				.findFirst().orElseThrow(() -> new BillPaymentException("The selected source account is unavailable."));
		requireSource(customer, source);
		if (source.getCurrentBalance().compareTo(amount) < 0 || source.getAvailableBalance().compareTo(amount) < 0) {
			throw new BillPaymentException("Insufficient account balance for this bill payment.");
		}

		source.setCurrentBalance(source.getCurrentBalance().subtract(amount));
		source.setAvailableBalance(source.getAvailableBalance().subtract(amount));
		bankAccountRepository.save(source);
		LocalDateTime paidAt = LocalDateTime.now();
		String reference = referenceNumber();
		BillPayment payment = billPaymentRepository.save(new BillPayment(customer, source, savedBiller, reference,
				provider, consumerReference, amount, source.getAvailableBalance(), clean(request.getDescription(), 140), paidAt));
		transactionRepository.save(new AccountTransaction(source, reference, TransactionDirection.DEBIT,
				AccountTransactionType.BILL_PAYMENT, amount, source.getAvailableBalance(), provider.getDisplayName(),
				masker.maskAccountNumber(consumerReference), clean(request.getDescription(), 140), paidAt));
		String message = String.format(Locale.ROOT, "LKR %s paid to %s (%s).",
				amount.toPlainString(), provider.getDisplayName(), masker.maskAccountNumber(consumerReference));
		notificationRepository.save(new CustomerNotification(customer.getUser(), NotificationType.ACCOUNT_NOTICE,
				"Bill payment completed", message, reference));
		auditLogRepository.save(new AuditLog(actor(actorUsername), "BILL_PAYMENT_COMPLETED", "BILL_PAYMENT", reference,
				null, BillPaymentStatus.COMPLETED.name(), message, paidAt));
		return details(payment);
	}

	@Override
	@Transactional(readOnly = true)
	public List<BillPaymentListView> getCustomerHistory(Long userId) {
		Customer customer = customer(userId);
		return billPaymentRepository.findTop50ByCustomerIdOrderByPaidAtDesc(customer.getId()).stream().map(this::summary).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public BillPaymentDetailsView getCustomerPayment(Long userId, String referenceNumber) {
		Customer customer = customer(userId);
		return billPaymentRepository.findByReferenceNumberAndCustomerId(trim(referenceNumber), customer.getId())
				.map(this::details).orElseThrow(() -> new BillPaymentException("Bill payment receipt was not found."));
	}

	@Override
	@Transactional(readOnly = true)
	public List<SavedBillerView> getSavedBillers(Long userId) { return savedBillers(customer(userId)); }

	@Override
	@Transactional
	public SavedBillerView saveBiller(Long userId, String actorUsername, SavedBillerRequest request) {
		Customer customer = customer(userId);
		requireActive(customer);
		if (request == null || request.getProvider() == null) throw new BillPaymentException("Select a service provider.");
		SavedBiller saved = createSavedBiller(customer, request.getProvider(), request.getNickname(),
				normalizeReference(request.getConsumerReference()));
		auditLogRepository.save(new AuditLog(actor(actorUsername), "SAVED_BILLER_CREATED", "SAVED_BILLER",
				String.valueOf(saved.getId()), null, SavedBillerStatus.ACTIVE.name(), saved.getProvider().getDisplayName(),
				LocalDateTime.now()));
		return savedBillerView(saved);
	}

	@Override
	@Transactional
	public void deleteBiller(Long userId, String actorUsername, Long savedBillerId) {
		Customer customer = customer(userId);
		SavedBiller biller = savedBillerRepository.findByIdAndCustomerIdAndStatus(savedBillerId, customer.getId(),
				SavedBillerStatus.ACTIVE).orElseThrow(() -> new BillPaymentException("Saved biller was not found."));
		biller.delete();
		savedBillerRepository.save(biller);
		auditLogRepository.save(new AuditLog(actor(actorUsername), "SAVED_BILLER_DELETED", "SAVED_BILLER",
				String.valueOf(biller.getId()), SavedBillerStatus.ACTIVE.name(), SavedBillerStatus.DELETED.name(),
				biller.getProvider().getDisplayName(), LocalDateTime.now()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<BillPaymentListView> getStaffPayments() {
		return billPaymentRepository.findTop100ByOrderByPaidAtDesc().stream().map(this::summary).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public BillPaymentDetailsView getStaffPayment(String referenceNumber) {
		return billPaymentRepository.findByReferenceNumber(trim(referenceNumber)).map(this::details)
				.orElseThrow(() -> new BillPaymentException("Bill payment receipt was not found."));
	}

	private Customer customer(Long userId) {
		if (userId == null) throw new CustomerProfileNotFoundException();
		return customerRepository.findByUserId(userId).orElseThrow(CustomerProfileNotFoundException::new);
	}

	private void requireActive(Customer c) {
		if (c.getStatus() != CustomerStatus.ACTIVE || c.getUser() == null || !c.getUser().isEnabled())
			throw new BillPaymentException("Customer access must be active before paying bills.");
	}

	private void requireSource(Customer c, BankAccount a) {
		if (a.getCustomer() == null || !Objects.equals(a.getCustomer().getId(), c.getId())
				|| a.getAccountStatus() != AccountStatus.ACTIVE || a.getCurrencyCode() != CurrencyCode.LKR)
			throw new BillPaymentException("Select an active LKR account that belongs to you.");
	}

	private void requirePin(Customer c, String pin) {
		String hash = c.getUser().getTransactionPinHash();
		if (pin == null || !pin.matches("\\d{4}") || hash == null || !passwordEncoder.matches(pin, hash))
			throw new BillPaymentException("Transaction PIN is incorrect.");
	}

	private BigDecimal amount(BigDecimal value) {
		if (value == null || value.scale() > 2 || value.compareTo(MIN_AMOUNT) < 0 || value.compareTo(MAX_AMOUNT) > 0)
			throw new BillPaymentException("Bill amount must be between LKR 10.00 and LKR 1,000,000.00.");
		return value.setScale(2, RoundingMode.UNNECESSARY);
	}

	private SavedBiller createSavedBiller(Customer customer, BillerProvider provider, String nickname, String reference) {
		String cleanNickname = clean(nickname, 80);
		if (cleanNickname == null || cleanNickname.length() < 2) throw new BillPaymentException("Enter a nickname for the biller.");
		if (savedBillerRepository.existsByCustomerIdAndProviderAndConsumerReferenceIgnoreCaseAndStatus(customer.getId(),
				provider, reference, SavedBillerStatus.ACTIVE)) throw new BillPaymentException("This biller is already saved.");
		return savedBillerRepository.save(new SavedBiller(customer, provider, cleanNickname, reference));
	}

	private String normalizeReference(String value) {
		String normalized = trim(value);
		if (normalized != null) normalized = normalized.replace(" ", "").toUpperCase(Locale.ROOT);
		if (normalized == null || !normalized.matches("[A-Z0-9+\\-/]{5,50}"))
			throw new BillPaymentException("Enter a valid consumer or service reference.");
		return normalized;
	}

	private List<SavedBillerView> savedBillers(Customer c) {
		return savedBillerRepository.findByCustomerIdAndStatusOrderByNicknameAsc(c.getId(), SavedBillerStatus.ACTIVE)
				.stream().map(this::savedBillerView).toList();
	}

	private SavedBillerView savedBillerView(SavedBiller b) {
		return new SavedBillerView(b.getId(), b.getNickname(), b.getProvider(), b.getProvider().getCategory(),
				b.getProvider().getDisplayName(), masker.maskAccountNumber(b.getConsumerReference()));
	}

	private BillPaymentListView summary(BillPayment p) {
		return new BillPaymentListView(p.getReferenceNumber(), p.getCustomer().getFullName(), p.getBillerNameSnapshot(),
				p.getCategory(), masker.maskAccountNumber(p.getConsumerReference()), p.getAmount(), p.getStatus(), p.getPaidAt());
	}

	private BillPaymentDetailsView details(BillPayment p) {
		return new BillPaymentDetailsView(p.getReferenceNumber(), p.getCustomer().getFullName(),
				p.getCustomer().getCustomerNumber(), masker.maskAccountNumber(p.getSourceAccount().getAccountNumber()),
				p.getBillerNameSnapshot(), p.getCategory(), masker.maskAccountNumber(p.getConsumerReference()), p.getAmount(),
				p.getStatus(), p.getSourceBalanceAfter(), p.getDescription(), p.getPaidAt());
	}

	private String referenceNumber() {
		String result;
		do result = "BIL" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
		while (billPaymentRepository.existsByReferenceNumber(result));
		return result;
	}

	private String actor(String value) { return trim(value) == null ? "UNKNOWN" : trim(value); }
	private String clean(String value, int max) {
		String result = trim(value);
		return result == null || result.length() <= max ? result : result.substring(0, max);
	}
	private String trim(String value) {
		if (value == null) return null;
		String result = value.trim();
		return result.isEmpty() ? null : result;
	}
}
