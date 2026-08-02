package com.digibank.service;

import com.digibank.dto.card.CardRequest;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Customer;
import com.digibank.entity.PaymentCard;
import com.digibank.entity.User;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CardStatus;
import com.digibank.enums.CardType;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import com.digibank.exception.CardException;
import com.digibank.repository.AuditLogRepository;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerNotificationRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.repository.PaymentCardRepository;
import com.digibank.service.impl.CardManagementServiceImpl;
import com.digibank.util.SensitiveDataMasker;
import com.digibank.security.CardDataProtector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardManagementServiceImplTest {

	@Mock private CustomerRepository customerRepository;
	@Mock private BankAccountRepository accountRepository;
	@Mock private PaymentCardRepository cardRepository;
	@Mock private CustomerNotificationRepository notificationRepository;
	@Mock private AuditLogRepository auditLogRepository;
	@Mock private PasswordEncoder passwordEncoder;

	private CardManagementServiceImpl service;
	private Customer customer;
	private BankAccount account;
	private CardDataProtector protector;

	@BeforeEach
	void setUp() {
		protector = new CardDataProtector("test-card-security-key-with-32-characters");
		service = new CardManagementServiceImpl(customerRepository, accountRepository, cardRepository,
				notificationRepository, auditLogRepository, passwordEncoder, new SensitiveDataMasker(), protector);
		User user = new User("customer", "customer@example.com", "hash");
		user.setTransactionPinHash("pin-hash");
		setId(user, 10L);
		customer = new Customer(user, "CUS100", "Test", "Customer", LocalDate.of(1990, 1, 1), Gender.OTHER,
				IdentityType.NATIONAL_ID, "NIC100", "+94710000000", "Address", "Colombo");
		setId(customer, 20L);
		customer.setStatus(CustomerStatus.ACTIVE);
		account = new BankAccount(customer, "111122223333", AccountType.SAVINGS, "COL001");
		setId(account, 30L);
		account.setAccountStatus(AccountStatus.ACTIVE);
		account.setCurrencyCode(CurrencyCode.LKR);
		when(customerRepository.findByUserId(10L)).thenReturn(Optional.of(customer));
		when(accountRepository.findByCustomerId(20L)).thenReturn(List.of(account));
		when(cardRepository.save(any(PaymentCard.class))).thenAnswer(call -> call.getArgument(0));
		when(passwordEncoder.matches("1234", "pin-hash")).thenReturn(true);
	}

	@Test
	void activeCustomerCanSubmitCardRequest() {
		var result = service.requestCard(10L, "customer", request(CardType.DEBIT));

		assertEquals(CardStatus.PENDING_REVIEW, result.status());
		assertEquals(CardType.DEBIT, result.cardType());
		assertEquals("Not issued", result.maskedCardNumber());
		assertTrue(result.requestNumber().startsWith("CRD"));
		verify(auditLogRepository).save(any());
	}

	@Test
	void duplicateOpenCardForSameAccountAndTypeIsRejected() {
		when(cardRepository.existsByBankAccountIdAndCardTypeAndStatusIn(30L, CardType.DEBIT, List.of(
				CardStatus.PENDING_REVIEW, CardStatus.INACTIVE, CardStatus.ACTIVE, CardStatus.BLOCKED))).thenReturn(true);

		assertThrows(CardException.class, () -> service.requestCard(10L, "customer", request(CardType.DEBIT)));
		verify(cardRepository, never()).save(any());
	}

	@Test
	void staffApprovalIssuesMaskedInactiveCardAndNotifiesCustomer() {
		PaymentCard card = pendingCard();
		when(cardRepository.findByRequestNumberForUpdate("CRDTEST")).thenReturn(Optional.of(card));

		service.approve("staff", "CRDTEST", "Identity checked");

		assertEquals(CardStatus.INACTIVE, card.getStatus());
		assertNotNull(card.getEncryptedCardNumber());
		assertTrue(card.getEncryptedCardNumber().startsWith("v1:"));
		assertEquals(16, protector.decrypt(card.getEncryptedCardNumber()).length());
		assertNotNull(card.getExpiryDate());
		verify(notificationRepository).save(any());
		verify(auditLogRepository).save(any());
	}

	@Test
	void approvedCardCanBeActivatedWithTransactionPin() {
		PaymentCard card = approvedCard();
		when(cardRepository.findByRequestNumberForUpdate("CRDTEST")).thenReturn(Optional.of(card));

		service.activate(10L, "customer", "CRDTEST", "1234");

		assertEquals(CardStatus.ACTIVE, card.getStatus());
		assertNotNull(card.getActivatedAt());
		verify(notificationRepository).save(any());
	}

	@Test
	void customerCanSecurelyRevealIssuedCardNumber() {
		PaymentCard card = approvedCard();
		when(cardRepository.findByRequestNumberAndCustomerUserId("CRDTEST", 10L)).thenReturn(Optional.of(card));

		String number = service.revealCardNumber(10L, "customer", "CRDTEST");

		assertEquals("4532123412341234", number);
		verify(auditLogRepository).save(any());
	}

	@Test
	void pendingCardNumberCannotBeRevealed() {
		when(cardRepository.findByRequestNumberAndCustomerUserId("CRDTEST", 10L))
				.thenReturn(Optional.of(pendingCard()));

		assertThrows(CardException.class, () -> service.revealCardNumber(10L, "customer", "CRDTEST"));
	}

	@Test
	void incorrectPinCannotActivateCard() {
		PaymentCard card = approvedCard();
		when(passwordEncoder.matches("9999", "pin-hash")).thenReturn(false);

		assertThrows(CardException.class, () -> service.activate(10L, "customer", "CRDTEST", "9999"));
		assertEquals(CardStatus.INACTIVE, card.getStatus());
		verify(cardRepository, never()).save(card);
	}

	@Test
	void customerCanBlockAndReactivateOwnBlock() {
		PaymentCard card = approvedCard();
		card.activate(LocalDateTime.now());
		when(cardRepository.findByRequestNumberForUpdate("CRDTEST")).thenReturn(Optional.of(card));

		service.blockByCustomer(10L, "customer", "CRDTEST", "1234");
		assertEquals(CardStatus.BLOCKED, card.getStatus());
		assertEquals("Blocked by customer", card.getBlockReason());

		service.reactivateByCustomer(10L, "customer", "CRDTEST", "1234");
		assertEquals(CardStatus.ACTIVE, card.getStatus());
	}

	@Test
	void customerCannotReactivateCardBlockedByStaff() {
		PaymentCard card = approvedCard();
		card.activate(LocalDateTime.now());
		card.block("Bank staff: suspected fraud", LocalDateTime.now());
		when(cardRepository.findByRequestNumberForUpdate("CRDTEST")).thenReturn(Optional.of(card));

		assertThrows(CardException.class,
				() -> service.reactivateByCustomer(10L, "customer", "CRDTEST", "1234"));
		assertEquals(CardStatus.BLOCKED, card.getStatus());
	}

	private CardRequest request(CardType type) {
		CardRequest request = new CardRequest();
		request.setAccountNumber(account.getAccountNumber());
		request.setCardType(type);
		return request;
	}

	private PaymentCard pendingCard() {
		PaymentCard card = new PaymentCard(customer, account, "CRDTEST", CardType.DEBIT, "TEST CUSTOMER",
				LocalDateTime.now());
		setId(card, 40L);
		return card;
	}

	private PaymentCard approvedCard() {
		PaymentCard card = pendingCard();
		String number = "4532123412341234";
		card.approve("staff", protector.encrypt(number), protector.hash(number), protector.lastFour(number),
				LocalDate.now().plusYears(5), "Approved", LocalDateTime.now());
		return card;
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
