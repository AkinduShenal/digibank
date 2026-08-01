package com.digibank.service;

import com.digibank.dto.transfer.InternalAccountLookupView;
import com.digibank.dto.transfer.TransferDetailsView;
import com.digibank.dto.transfer.TransferFormView;
import com.digibank.dto.transfer.TransferRequest;
import com.digibank.entity.AuditLog;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Beneficiary;
import com.digibank.entity.Customer;
import com.digibank.entity.FundTransfer;
import com.digibank.entity.User;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;
import com.digibank.enums.BeneficiaryVerificationStatus;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import com.digibank.enums.TransferStatus;
import com.digibank.enums.TransferRecipientType;
import com.digibank.enums.TransferType;
import com.digibank.exception.TransferException;
import com.digibank.exception.TransferNotFoundException;
import com.digibank.repository.AuditLogRepository;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.BeneficiaryRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.repository.FundTransferRepository;
import com.digibank.service.impl.TransferServiceImpl;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferServiceImplTest {

	private static final Long USER_ID = 10L;
	private static final Long CUSTOMER_ID = 20L;
	private static final Long BENEFICIARY_ID = 40L;
	private static final String SOURCE_NUMBER = "111122223333";
	private static final String DESTINATION_NUMBER = "999988887777";

	@Mock private CustomerRepository customerRepository;
	@Mock private BankAccountRepository bankAccountRepository;
	@Mock private BeneficiaryRepository beneficiaryRepository;
	@Mock private FundTransferRepository fundTransferRepository;
	@Mock private AuditLogRepository auditLogRepository;
	@Mock private PasswordEncoder passwordEncoder;

	private TransferServiceImpl service;
	private Customer customer;
	private BankAccount source;

	@BeforeEach
	void setUp() {
		service = new TransferServiceImpl(customerRepository, bankAccountRepository, beneficiaryRepository,
				fundTransferRepository, auditLogRepository, passwordEncoder, new SensitiveDataMasker());
		customer = customer(USER_ID, CUSTOMER_ID, "Sender", "Customer");
		source = account(customer, 30L, SOURCE_NUMBER, new BigDecimal("1000.00"));
		when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.of(customer));
		when(passwordEncoder.matches("1234", "encoded-pin")).thenReturn(true);
		when(fundTransferRepository.save(any(FundTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void internalTransferAtomicallyDebitsSenderAndCreditsReceiver() {
		Customer receiverCustomer = customer(99L, 88L, "Receiver", "Customer");
		BankAccount destination = account(receiverCustomer, 31L, DESTINATION_NUMBER, new BigDecimal("200.00"));
		Beneficiary beneficiary = beneficiary(BeneficiaryType.INTERNAL, BeneficiaryVerificationStatus.VERIFIED);
		whenBeneficiary(beneficiary);
		when(bankAccountRepository.findAllByAccountNumberInForUpdate(any()))
				.thenReturn(List.of(source, destination));

		TransferDetailsView result = service.transfer(USER_ID, "customer", request("250.00"));

		assertEquals(new BigDecimal("750.00"), source.getAvailableBalance());
		assertEquals(new BigDecimal("750.00"), source.getCurrentBalance());
		assertEquals(new BigDecimal("450.00"), destination.getAvailableBalance());
		assertEquals(TransferType.INTERNAL, result.transferType());
		assertEquals(TransferStatus.COMPLETED, result.status());
		assertEquals(new BigDecimal("750.00"), result.sourceBalanceAfter());
		verify(bankAccountRepository).saveAll(any());
		assertEquals("FUND_TRANSFER_COMPLETED", capturedAudit().getAction());
	}

	@Test
	void externalTransferDebitsSenderAndCreatesCompletedRecord() {
		Beneficiary beneficiary = beneficiary(BeneficiaryType.EXTERNAL, BeneficiaryVerificationStatus.VERIFIED);
		whenBeneficiary(beneficiary);
		when(bankAccountRepository.findAllByAccountNumberInForUpdate(List.of(SOURCE_NUMBER)))
				.thenReturn(List.of(source));

		TransferDetailsView result = service.transfer(USER_ID, "customer", request("100.00"));

		assertEquals(new BigDecimal("900.00"), source.getAvailableBalance());
		assertEquals(TransferType.EXTERNAL, result.transferType());
		assertEquals(TransferStatus.COMPLETED, result.status());
		assertNotNull(result.referenceNumber());
	}

	@Test
	void directInternalTransferDoesNotRequireOrSaveBeneficiary() {
		Customer receiverCustomer = customer(99L, 88L, "Receiver", "Customer");
		BankAccount destination = account(receiverCustomer, 31L, DESTINATION_NUMBER, new BigDecimal("200.00"));
		when(bankAccountRepository.findAllByAccountNumberInForUpdate(any()))
				.thenReturn(List.of(source, destination));

		TransferDetailsView result = service.transfer(USER_ID, "customer", directRequest("250.00"));

		assertEquals(new BigDecimal("750.00"), source.getAvailableBalance());
		assertEquals(new BigDecimal("450.00"), destination.getAvailableBalance());
		assertEquals("Receiver Customer", result.beneficiaryName());
		assertEquals(TransferType.INTERNAL, result.transferType());
		ArgumentCaptor<FundTransfer> transferCaptor = ArgumentCaptor.forClass(FundTransfer.class);
		verify(fundTransferRepository).save(transferCaptor.capture());
		assertNull(transferCaptor.getValue().getBeneficiary());
	}

	@Test
	void directInternalAccountLookupReturnsNameOnlyForEligibleExactAccount() {
		Customer receiverCustomer = customer(99L, 88L, "Receiver", "Customer");
		BankAccount destination = account(receiverCustomer, 31L, DESTINATION_NUMBER, new BigDecimal("200.00"));
		when(bankAccountRepository.findByAccountNumber(DESTINATION_NUMBER)).thenReturn(Optional.of(destination));

		InternalAccountLookupView result = service.lookupInternalAccount(USER_ID, DESTINATION_NUMBER);

		assertTrue(result.available());
		assertEquals("Receiver Customer", result.accountHolderName());
		assertEquals("********7777", result.maskedAccountNumber());
	}

	@Test
	void directInternalAccountLookupDoesNotExposeOwnOrInvalidAccount() {
		when(bankAccountRepository.findByAccountNumber(SOURCE_NUMBER)).thenReturn(Optional.of(source));

		assertFalse(service.lookupInternalAccount(USER_ID, SOURCE_NUMBER).available());
		assertFalse(service.lookupInternalAccount(USER_ID, "123").available());
	}

	@Test
	void pendingBeneficiaryIsRejectedBeforeAccountLocking() {
		whenBeneficiary(beneficiary(BeneficiaryType.EXTERNAL, BeneficiaryVerificationStatus.PENDING));

		assertThrows(TransferException.class, () -> service.transfer(USER_ID, "customer", request("100.00")));

		verify(bankAccountRepository, never()).findAllByAccountNumberInForUpdate(any());
		verify(fundTransferRepository, never()).save(any());
	}

	@Test
	void amountAboveBeneficiaryLimitIsRejected() {
		whenBeneficiary(beneficiary(BeneficiaryType.EXTERNAL, BeneficiaryVerificationStatus.VERIFIED));

		assertThrows(TransferException.class, () -> service.transfer(USER_ID, "customer", request("500.01")));

		verify(fundTransferRepository, never()).save(any());
	}

	@Test
	void incorrectTransactionPinIsRejected() {
		whenBeneficiary(beneficiary(BeneficiaryType.EXTERNAL, BeneficiaryVerificationStatus.VERIFIED));
		when(passwordEncoder.matches("1234", "encoded-pin")).thenReturn(false);

		TransferException exception = assertThrows(TransferException.class,
				() -> service.transfer(USER_ID, "customer", request("100.00")));

		assertEquals("Transaction PIN is incorrect.", exception.getMessage());
		verify(fundTransferRepository, never()).save(any());
	}

	@Test
	void insufficientBalanceIsRejectedWithoutMutation() {
		whenBeneficiary(beneficiary(BeneficiaryType.EXTERNAL, BeneficiaryVerificationStatus.VERIFIED));
		source.setAvailableBalance(new BigDecimal("400.00"));
		source.setCurrentBalance(new BigDecimal("400.00"));
		when(bankAccountRepository.findAllByAccountNumberInForUpdate(List.of(SOURCE_NUMBER)))
				.thenReturn(List.of(source));

		assertThrows(TransferException.class, () -> service.transfer(USER_ID, "customer", request("500.00")));

		assertEquals(new BigDecimal("400.00"), source.getAvailableBalance());
		verify(fundTransferRepository, never()).save(any());
	}

	@Test
	void frozenSourceAccountIsRejected() {
		whenBeneficiary(beneficiary(BeneficiaryType.EXTERNAL, BeneficiaryVerificationStatus.VERIFIED));
		source.setAccountStatus(AccountStatus.FROZEN);
		when(bankAccountRepository.findAllByAccountNumberInForUpdate(List.of(SOURCE_NUMBER)))
				.thenReturn(List.of(source));

		assertThrows(TransferException.class, () -> service.transfer(USER_ID, "customer", request("100.00")));
	}

	@Test
	void sourceAccountMustBelongToAuthenticatedCustomer() {
		whenBeneficiary(beneficiary(BeneficiaryType.EXTERNAL, BeneficiaryVerificationStatus.VERIFIED));
		BankAccount otherAccount = account(customer(55L, 66L, "Other", "Customer"), 67L, SOURCE_NUMBER,
				new BigDecimal("1000.00"));
		when(bankAccountRepository.findAllByAccountNumberInForUpdate(List.of(SOURCE_NUMBER)))
				.thenReturn(List.of(otherAccount));

		assertThrows(TransferException.class, () -> service.transfer(USER_ID, "customer", request("100.00")));
	}

	@Test
	void formContainsOnlyActiveAccountsAndVerifiedBeneficiaries() {
		BankAccount frozen = account(customer, 32L, "444455556666", new BigDecimal("200.00"));
		frozen.setAccountStatus(AccountStatus.FROZEN);
		Beneficiary beneficiary = beneficiary(BeneficiaryType.EXTERNAL, BeneficiaryVerificationStatus.VERIFIED);
		when(bankAccountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(frozen, source));
		when(beneficiaryRepository.findAllByCustomerIdAndStatusAndVerificationStatusOrderByBeneficiaryNameAsc(
				CUSTOMER_ID, BeneficiaryStatus.ACTIVE, BeneficiaryVerificationStatus.VERIFIED))
				.thenReturn(List.of(beneficiary));

		TransferFormView form = service.getTransferForm(USER_ID);

		assertEquals(1, form.accounts().size());
		assertEquals(SOURCE_NUMBER, form.accounts().get(0).accountNumber());
		assertEquals(1, form.beneficiaries().size());
	}

	@Test
	void transferDetailsAreRestrictedToOwningCustomer() {
		when(fundTransferRepository.findByReferenceNumberAndCustomerId("TRF-NOT-MINE", CUSTOMER_ID))
				.thenReturn(Optional.empty());

		assertThrows(TransferNotFoundException.class, () -> service.getTransfer(USER_ID, "TRF-NOT-MINE"));
	}

	private void whenBeneficiary(Beneficiary beneficiary) {
		when(beneficiaryRepository.findByIdAndCustomerIdAndStatusNot(eq(BENEFICIARY_ID), eq(CUSTOMER_ID),
				eq(BeneficiaryStatus.DELETED))).thenReturn(Optional.of(beneficiary));
	}

	private Beneficiary beneficiary(BeneficiaryType type, BeneficiaryVerificationStatus verificationStatus) {
		Beneficiary beneficiary = new Beneficiary(customer, "Receiver Customer", type == BeneficiaryType.INTERNAL
				? "DigiBank" : "Example Bank", type == BeneficiaryType.INTERNAL ? "DIGIBANK" : "EXB01",
				DESTINATION_NUMBER, DESTINATION_NUMBER, BeneficiaryAccountType.SAVINGS, type);
		setId(beneficiary, BENEFICIARY_ID);
		beneficiary.setStatus(BeneficiaryStatus.ACTIVE);
		beneficiary.setVerificationStatus(verificationStatus);
		beneficiary.setTransferLimit(new BigDecimal("500.00"));
		return beneficiary;
	}

	private TransferRequest request(String amount) {
		TransferRequest request = new TransferRequest();
		request.setSourceAccountNumber(SOURCE_NUMBER);
		request.setBeneficiaryId(BENEFICIARY_ID);
		request.setAmount(new BigDecimal(amount));
		request.setDescription("Invoice 100");
		request.setTransactionPin("1234");
		return request;
	}

	private TransferRequest directRequest(String amount) {
		TransferRequest request = new TransferRequest();
		request.setSourceAccountNumber(SOURCE_NUMBER);
		request.setRecipientType(TransferRecipientType.DIGIBANK_ACCOUNT);
		request.setDestinationAccountNumber(DESTINATION_NUMBER);
		request.setAmount(new BigDecimal(amount));
		request.setDescription("Direct DigiBank payment");
		request.setTransactionPin("1234");
		return request;
	}

	private Customer customer(Long userId, Long customerId, String firstName, String lastName) {
		User user = new User(firstName.toLowerCase(), firstName.toLowerCase() + "@example.com", "password");
		setId(user, userId);
		user.setTransactionPinHash("encoded-pin");
		user.setEnabled(true);
		Customer created = new Customer(user, "CUS" + customerId, firstName, lastName, LocalDate.of(1990, 1, 1),
				Gender.OTHER, IdentityType.NATIONAL_ID, "NIC" + customerId, "+94712345678", "Address", "Colombo");
		setId(created, customerId);
		created.setStatus(CustomerStatus.ACTIVE);
		return created;
	}

	private BankAccount account(Customer owner, Long id, String number, BigDecimal balance) {
		BankAccount account = new BankAccount(owner, number, AccountType.SAVINGS, "COL001");
		setId(account, id);
		account.setAccountStatus(AccountStatus.ACTIVE);
		account.setCurrencyCode(CurrencyCode.LKR);
		account.setAvailableBalance(balance);
		account.setCurrentBalance(balance);
		return account;
	}

	private AuditLog capturedAudit() {
		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
		verify(auditLogRepository).save(captor.capture());
		return captor.getValue();
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
