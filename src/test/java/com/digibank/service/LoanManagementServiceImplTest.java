package com.digibank.service;

import com.digibank.dto.loan.LoanApplicationRequest;
import com.digibank.dto.loan.LoanRepaymentRequest;
import com.digibank.entity.AccountTransaction;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Customer;
import com.digibank.entity.LoanApplication;
import com.digibank.entity.LoanRepaymentSchedule;
import com.digibank.entity.User;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import com.digibank.enums.LoanStatus;
import com.digibank.enums.LoanType;
import com.digibank.exception.LoanException;
import com.digibank.repository.AccountTransactionRepository;
import com.digibank.repository.AuditLogRepository;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerNotificationRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.repository.LoanApplicationRepository;
import com.digibank.repository.LoanRepaymentScheduleRepository;
import com.digibank.service.impl.LoanManagementServiceImpl;
import com.digibank.util.SensitiveDataMasker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoanManagementServiceImplTest {

	@Mock private CustomerRepository customerRepository;
	@Mock private BankAccountRepository accountRepository;
	@Mock private LoanApplicationRepository loanRepository;
	@Mock private LoanRepaymentScheduleRepository scheduleRepository;
	@Mock private AccountTransactionRepository transactionRepository;
	@Mock private CustomerNotificationRepository notificationRepository;
	@Mock private AuditLogRepository auditLogRepository;
	@Mock private PasswordEncoder passwordEncoder;

	private LoanManagementServiceImpl service;
	private Customer customer;
	private BankAccount account;

	@BeforeEach
	void setUp() {
		service = new LoanManagementServiceImpl(customerRepository, accountRepository, loanRepository,
				scheduleRepository, transactionRepository, notificationRepository, auditLogRepository,
				passwordEncoder, new SensitiveDataMasker());
		User user = new User("customer", "customer@example.com", "hash");
		user.setTransactionPinHash("encoded-pin");
		setId(user, 10L);
		customer = new Customer(user, "CUS100", "Test", "Customer", LocalDate.of(1990, 1, 1), Gender.OTHER,
				IdentityType.NATIONAL_ID, "NIC100", "+94710000000", "Address", "Colombo");
		setId(customer, 20L);
		customer.setStatus(CustomerStatus.ACTIVE);
		account = new BankAccount(customer, "111122223333", AccountType.SAVINGS, "COL001");
		setId(account, 30L);
		account.setAccountStatus(AccountStatus.ACTIVE);
		account.setCurrencyCode(CurrencyCode.LKR);
		account.setAvailableBalance(new BigDecimal("1000.00"));
		account.setCurrentBalance(new BigDecimal("1000.00"));
		when(customerRepository.findByUserId(10L)).thenReturn(Optional.of(customer));
		when(accountRepository.findByCustomerId(20L)).thenReturn(List.of(account));
		when(loanRepository.save(any(LoanApplication.class))).thenAnswer(call -> call.getArgument(0));
		when(passwordEncoder.matches("1234", "encoded-pin")).thenReturn(true);
	}

	@Test
	void customerCanSubmitAffordableLoanApplication() {
		var result = service.apply(10L, "customer", request("100000.00", "50000.00"));

		assertEquals(LoanStatus.PENDING_REVIEW, result.status());
		assertEquals(LoanType.PERSONAL, result.loanType());
		assertEquals(new BigDecimal("8884.88"), result.monthlyInstallment());
		verify(loanRepository).save(any(LoanApplication.class));
		verify(auditLogRepository).save(any());
	}

	@Test
	void unaffordableApplicationIsRejected() {
		assertThrows(LoanException.class, () -> service.apply(10L, "customer", request("100000.00", "10000.00")));
		verify(loanRepository, never()).save(any());
	}

	@Test
	void pendingApplicationCanBeApprovedAndAtomicallyDisbursed() {
		LoanApplication loan = loan();
		when(loanRepository.findByApplicationNumberForUpdate("LONTEST")).thenReturn(Optional.of(loan));
		when(accountRepository.findAllByAccountNumberInForUpdate(List.of(account.getAccountNumber())))
				.thenReturn(List.of(account));

		service.approveAndDisburse("staff", "LONTEST", new BigDecimal("100000.00"), "Approved");

		assertEquals(LoanStatus.DISBURSED, loan.getStatus());
		assertEquals(new BigDecimal("101000.00"), account.getAvailableBalance());
		verify(transactionRepository).save(any(AccountTransaction.class));
		verify(notificationRepository).save(any());
		ArgumentCaptor<List<LoanRepaymentSchedule>> captor = ArgumentCaptor.forClass(List.class);
		verify(scheduleRepository).saveAll(captor.capture());
		assertEquals(12, captor.getValue().size());
		assertTrue(captor.getValue().stream().allMatch(row -> row.getTotalAmount().signum() > 0));
	}

	@Test
	void disbursedLoanCannotBeApprovedTwice() {
		LoanApplication loan = loan();
		loan.disburse("staff", "Approved", new BigDecimal("100000.00"), new BigDecimal("8884.88"),
				java.time.LocalDateTime.now());
		when(loanRepository.findByApplicationNumberForUpdate("LONTEST")).thenReturn(Optional.of(loan));

		assertThrows(LoanException.class,
				() -> service.approveAndDisburse("staff", "LONTEST", new BigDecimal("100000.00"), "Again"));
		verify(accountRepository, never()).save(any());
	}

	@Test
	void nextInstallmentCanBePaidWithPinAndCreatesLedgerEntry() {
		LoanApplication loan = loan();
		loan.disburse("staff", "Approved", new BigDecimal("100000.00"), new BigDecimal("8884.88"),
				java.time.LocalDateTime.now());
		LoanRepaymentSchedule installment = new LoanRepaymentSchedule(loan, 1, LocalDate.now().plusMonths(1),
				new BigDecimal("7884.88"), new BigDecimal("1000.00"), new BigDecimal("8884.88"));
		setId(installment, 50L);
		account.setAvailableBalance(new BigDecimal("20000.00"));
		account.setCurrentBalance(new BigDecimal("20000.00"));
		when(loanRepository.findByApplicationNumberForUpdate("LONTEST")).thenReturn(Optional.of(loan));
		when(scheduleRepository.findUnpaid(any(), any())).thenReturn(List.of(installment));
		when(scheduleRepository.findInstallmentForUpdate(40L, 1)).thenReturn(Optional.of(installment));
		when(accountRepository.findAllByAccountNumberInForUpdate(List.of(account.getAccountNumber())))
				.thenReturn(List.of(account));
		when(scheduleRepository.countByLoanApplicationIdAndStatusNot(any(), any())).thenReturn(11L);

		String reference = service.payInstallment(10L, "customer", "LONTEST", 1, repaymentRequest("1234"));

		assertTrue(reference.startsWith("LRP"));
		assertEquals(new BigDecimal("11115.12"), account.getAvailableBalance());
		assertEquals(com.digibank.enums.RepaymentStatus.PAID, installment.getStatus());
		verify(transactionRepository).save(any(AccountTransaction.class));
		verify(notificationRepository).save(any());
	}

	@Test
	void incorrectPinCannotDebitRepaymentAccount() {
		when(passwordEncoder.matches("9999", "encoded-pin")).thenReturn(false);
		assertThrows(LoanException.class,
				() -> service.payInstallment(10L, "customer", "LONTEST", 1, repaymentRequest("9999")));
		verify(accountRepository, never()).save(any());
	}

	@Test
	void finalInstallmentClosesTheLoan() {
		LoanApplication loan = loan();
		loan.disburse("staff", "Approved", new BigDecimal("100000.00"), new BigDecimal("8884.88"),
				java.time.LocalDateTime.now());
		LoanRepaymentSchedule installment = new LoanRepaymentSchedule(loan, 12, LocalDate.now(),
				new BigDecimal("8796.91"), new BigDecimal("87.97"), new BigDecimal("8884.88"));
		setId(installment, 61L);
		account.setAvailableBalance(new BigDecimal("10000.00"));
		account.setCurrentBalance(new BigDecimal("10000.00"));
		when(loanRepository.findByApplicationNumberForUpdate("LONTEST")).thenReturn(Optional.of(loan));
		when(scheduleRepository.findUnpaid(any(), any())).thenReturn(List.of(installment));
		when(scheduleRepository.findInstallmentForUpdate(40L, 12)).thenReturn(Optional.of(installment));
		when(accountRepository.findAllByAccountNumberInForUpdate(List.of(account.getAccountNumber())))
				.thenReturn(List.of(account));
		when(scheduleRepository.countByLoanApplicationIdAndStatusNot(40L,
				com.digibank.enums.RepaymentStatus.PAID)).thenReturn(0L);

		service.payInstallment(10L, "customer", "LONTEST", 12, repaymentRequest("1234"));

		assertEquals(LoanStatus.CLOSED, loan.getStatus());
		verify(loanRepository).save(loan);
	}

	private LoanApplicationRequest request(String amount, String income) {
		LoanApplicationRequest request = new LoanApplicationRequest();
		request.setAccountNumber(account.getAccountNumber());
		request.setLoanType(LoanType.PERSONAL);
		request.setRequestedAmount(new BigDecimal(amount));
		request.setTermMonths(12);
		request.setMonthlyIncome(new BigDecimal(income));
		request.setEmploymentStatus("Permanent employee");
		request.setPurpose("Personal home improvements");
		return request;
	}

	private LoanRepaymentRequest repaymentRequest(String pin) {
		LoanRepaymentRequest request = new LoanRepaymentRequest();
		request.setAccountNumber(account.getAccountNumber());
		request.setTransactionPin(pin);
		return request;
	}

	private LoanApplication loan() {
		LoanApplication loan = new LoanApplication(customer, account, "LONTEST", LoanType.PERSONAL,
				new BigDecimal("100000.00"), new BigDecimal("12.00"), 12, new BigDecimal("8884.88"),
				new BigDecimal("50000.00"), "Permanent employee", "Personal home improvements");
		setId(loan, 40L);
		return loan;
	}

	private void setId(Object entity, Long id) {
		try {
			Method method = entity.getClass().getSuperclass().getDeclaredMethod("setId", Long.class);
			method.setAccessible(true);
			method.invoke(entity, id);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
