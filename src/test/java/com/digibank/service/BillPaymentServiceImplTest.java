package com.digibank.service;

import com.digibank.dto.bill.BillPaymentRequest;
import com.digibank.dto.bill.SavedBillerRequest;
import com.digibank.entity.*;
import com.digibank.enums.*;
import com.digibank.exception.BillPaymentException;
import com.digibank.repository.*;
import com.digibank.service.impl.BillPaymentServiceImpl;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillPaymentServiceImplTest {
	@Mock CustomerRepository customerRepository;
	@Mock BankAccountRepository accountRepository;
	@Mock SavedBillerRepository savedBillerRepository;
	@Mock BillPaymentRepository paymentRepository;
	@Mock AccountTransactionRepository transactionRepository;
	@Mock CustomerNotificationRepository notificationRepository;
	@Mock AuditLogRepository auditLogRepository;
	@Mock PasswordEncoder passwordEncoder;
	private BillPaymentServiceImpl service;
	private Customer customer;
	private BankAccount account;

	@BeforeEach void setUp() {
		service = new BillPaymentServiceImpl(customerRepository, accountRepository, savedBillerRepository,
				paymentRepository, transactionRepository, notificationRepository, auditLogRepository,
				passwordEncoder, new SensitiveDataMasker());
		User user = new User("customer", "customer@digibank.test", "hash");
		user.setTransactionPinHash("pin-hash"); setId(user, 10L);
		customer = new Customer(user, "CUS100", "Test", "Customer", LocalDate.of(1990, 1, 1), Gender.OTHER,
				IdentityType.NATIONAL_ID, "NIC100", "+94710000000", "Address", "Colombo");
		setId(customer, 20L); customer.setStatus(CustomerStatus.ACTIVE);
		account = new BankAccount(customer, "111122223333", AccountType.SAVINGS, "COL001");
		setId(account, 30L); account.setAccountStatus(AccountStatus.ACTIVE); account.setCurrencyCode(CurrencyCode.LKR);
		account.setCurrentBalance(new BigDecimal("1000.00")); account.setAvailableBalance(new BigDecimal("1000.00"));
		when(customerRepository.findByUserId(10L)).thenReturn(Optional.of(customer));
		when(passwordEncoder.matches("1234", "pin-hash")).thenReturn(true);
		when(accountRepository.findAllByAccountNumberInForUpdate(List.of("111122223333"))).thenReturn(List.of(account));
		when(paymentRepository.save(any(BillPayment.class))).thenAnswer(i -> i.getArgument(0));
		when(savedBillerRepository.save(any(SavedBiller.class))).thenAnswer(i -> { SavedBiller b = i.getArgument(0); if (b.getId() == null) setId(b, 40L); return b; });
	}

	@Test void completedPaymentDebitsAccountAndWritesAllRecords() {
		var result = service.pay(10L, "customer", newRequest("250.00"));

		assertEquals(new BigDecimal("750.00"), account.getAvailableBalance());
		assertEquals(BillPaymentStatus.COMPLETED, result.status());
		assertTrue(result.referenceNumber().startsWith("BIL"));
		ArgumentCaptor<AccountTransaction> ledger = ArgumentCaptor.forClass(AccountTransaction.class);
		verify(transactionRepository).save(ledger.capture());
		assertEquals(AccountTransactionType.BILL_PAYMENT, ledger.getValue().getTransactionType());
		verify(notificationRepository).save(any(CustomerNotification.class));
		verify(auditLogRepository).save(any(AuditLog.class));
	}

	@Test void incorrectPinDoesNotDebitAccount() {
		when(passwordEncoder.matches("1234", "pin-hash")).thenReturn(false);
		assertThrows(BillPaymentException.class, () -> service.pay(10L, "customer", newRequest("250.00")));
		assertEquals(new BigDecimal("1000.00"), account.getAvailableBalance());
		verify(paymentRepository, never()).save(any());
	}

	@Test void insufficientBalanceIsRejected() {
		assertThrows(BillPaymentException.class, () -> service.pay(10L, "customer", newRequest("1500.00")));
		assertEquals(new BigDecimal("1000.00"), account.getAvailableBalance());
	}

	@Test void savedBillerMustBelongToAuthenticatedCustomer() {
		BillPaymentRequest request = newRequest("100.00");
		request.setSelectionType(BillerSelectionType.SAVED_BILLER); request.setSavedBillerId(99L);
		assertThrows(BillPaymentException.class, () -> service.pay(10L, "customer", request));
		verify(accountRepository, never()).findAllByAccountNumberInForUpdate(any());
	}

	@Test void duplicateSavedBillerIsRejectedAndDeletionIsSoft() {
		SavedBillerRequest request = new SavedBillerRequest(); request.setProvider(BillerProvider.CEB);
		request.setNickname("Home"); request.setConsumerReference("ACC-12345");
		when(savedBillerRepository.existsByCustomerIdAndProviderAndConsumerReferenceIgnoreCaseAndStatus(
				20L, BillerProvider.CEB, "ACC-12345", SavedBillerStatus.ACTIVE)).thenReturn(true);
		assertThrows(BillPaymentException.class, () -> service.saveBiller(10L, "customer", request));

		SavedBiller biller = new SavedBiller(customer, BillerProvider.CEB, "Home", "ACC-12345"); setId(biller, 40L);
		when(savedBillerRepository.findByIdAndCustomerIdAndStatus(40L, 20L, SavedBillerStatus.ACTIVE))
				.thenReturn(Optional.of(biller));
		service.deleteBiller(10L, "customer", 40L);
		assertEquals(SavedBillerStatus.DELETED, biller.getStatus());
		verify(savedBillerRepository).save(biller);
	}

	private BillPaymentRequest newRequest(String amount) {
		BillPaymentRequest request = new BillPaymentRequest(); request.setSourceAccountNumber("111122223333");
		request.setSelectionType(BillerSelectionType.NEW_BILLER); request.setProvider(BillerProvider.CEB);
		request.setConsumerReference("ACC-12345"); request.setAmount(new BigDecimal(amount));
		request.setTransactionPin("1234"); request.setDescription("August bill"); return request;
	}

	private void setId(Object entity, Long id) {
		try { Method m = entity.getClass().getSuperclass().getDeclaredMethod("setId", Long.class); m.setAccessible(true); m.invoke(entity, id); }
		catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
	}
}
