package com.digibank.service.impl;

import com.digibank.dto.card.CardAccountOption;
import com.digibank.dto.card.CardRequest;
import com.digibank.dto.card.CardRequestFormView;
import com.digibank.dto.card.CardView;
import com.digibank.entity.AuditLog;
import com.digibank.entity.BankAccount;
import com.digibank.entity.Customer;
import com.digibank.entity.CustomerNotification;
import com.digibank.entity.PaymentCard;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.CardStatus;
import com.digibank.enums.CardType;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.NotificationType;
import com.digibank.exception.CardException;
import com.digibank.repository.AuditLogRepository;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerNotificationRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.repository.PaymentCardRepository;
import com.digibank.service.CardManagementService;
import com.digibank.util.SensitiveDataMasker;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CardManagementServiceImpl implements CardManagementService {

	private static final List<CardStatus> OPEN_STATUSES = List.of(
			CardStatus.PENDING_REVIEW, CardStatus.INACTIVE, CardStatus.ACTIVE, CardStatus.BLOCKED);
	private static final int GENERATION_ATTEMPTS = 20;

	private final CustomerRepository customerRepository;
	private final BankAccountRepository accountRepository;
	private final PaymentCardRepository cardRepository;
	private final CustomerNotificationRepository notificationRepository;
	private final AuditLogRepository auditLogRepository;
	private final PasswordEncoder passwordEncoder;
	private final SensitiveDataMasker dataMasker;
	private final SecureRandom secureRandom = new SecureRandom();

	public CardManagementServiceImpl(CustomerRepository customerRepository, BankAccountRepository accountRepository,
			PaymentCardRepository cardRepository, CustomerNotificationRepository notificationRepository,
			AuditLogRepository auditLogRepository, PasswordEncoder passwordEncoder, SensitiveDataMasker dataMasker) {
		this.customerRepository = customerRepository;
		this.accountRepository = accountRepository;
		this.cardRepository = cardRepository;
		this.notificationRepository = notificationRepository;
		this.auditLogRepository = auditLogRepository;
		this.passwordEncoder = passwordEncoder;
		this.dataMasker = dataMasker;
	}

	@Override
	public CardRequestFormView getRequestForm(Long userId) {
		Customer customer = customer(userId);
		return new CardRequestFormView(eligibleAccounts(customer).stream()
				.map(account -> new CardAccountOption(account.getAccountNumber(), account.getAccountType().name()))
				.toList(), CardType.values());
	}

	@Override
	@Transactional
	public CardView requestCard(Long userId, String actorUsername, CardRequest request) {
		Customer customer = customer(userId);
		if (customer.getStatus() != CustomerStatus.ACTIVE) {
			throw new CardException("Only active customers can request a card.");
		}
		if (request == null || request.getCardType() == null) {
			throw new CardException("Select a card type.");
		}
		String accountNumber = clean(request.getAccountNumber());
		BankAccount account = eligibleAccounts(customer).stream()
				.filter(candidate -> candidate.getAccountNumber().equals(accountNumber))
				.findFirst().orElseThrow(() -> new CardException("Select an active LKR account owned by you."));
		if (cardRepository.existsByBankAccountIdAndCardTypeAndStatusIn(account.getId(), request.getCardType(),
				OPEN_STATUSES)) {
			throw new CardException("This account already has an open request or usable card of the selected type.");
		}
		LocalDateTime now = LocalDateTime.now();
		PaymentCard card = new PaymentCard(customer, account, requestNumber(), request.getCardType(),
				cardholderName(customer), now);
		cardRepository.save(card);
		audit(actorUsername, "CARD_REQUEST_SUBMITTED", card, null, CardStatus.PENDING_REVIEW,
				"Customer submitted a " + request.getCardType().name() + " card request.", now);
		return view(card);
	}

	@Override
	public List<CardView> getCustomerCards(Long userId) {
		customer(userId);
		return cardRepository.findByCustomerUserIdOrderByRequestedAtDesc(userId).stream().map(this::view).toList();
	}

	@Override
	public CardView getCustomerCard(Long userId, String requestNumber) {
		return view(customerCard(userId, requestNumber));
	}

	@Override
	@Transactional
	public String revealCardNumber(Long userId, String actorUsername, String requestNumber) {
		PaymentCard card = customerCard(userId, requestNumber);
		if (card.getCardNumber() == null || card.getStatus() == CardStatus.PENDING_REVIEW
				|| card.getStatus() == CardStatus.REJECTED) {
			throw new CardException("The card number is not available until the card request is approved.");
		}
		audit(actorUsername, "CARD_NUMBER_VIEWED", card, card.getStatus(), card.getStatus(),
				"Customer securely viewed the issued card number.", LocalDateTime.now());
		return card.getCardNumber();
	}

	@Override
	@Transactional
	public void activate(Long userId, String actorUsername, String requestNumber, String transactionPin) {
		Customer customer = customer(userId);
		requireTransactionPin(customer, transactionPin);
		PaymentCard card = lockedCustomerCard(customer, requestNumber);
		if (card.getStatus() != CardStatus.INACTIVE) {
			throw new CardException("Only an approved inactive card can be activated.");
		}
		if (card.getExpiryDate() == null || card.getExpiryDate().isBefore(LocalDate.now())) {
			throw new CardException("This card has expired and cannot be activated.");
		}
		LocalDateTime now = LocalDateTime.now();
		card.activate(now);
		cardRepository.save(card);
		notify(card, "Card activated", "Your card " + masked(card) + " is now active.");
		audit(actorUsername, "CARD_ACTIVATED", card, CardStatus.INACTIVE, CardStatus.ACTIVE,
				"Customer activated the card.", now);
	}

	@Override
	@Transactional
	public void blockByCustomer(Long userId, String actorUsername, String requestNumber, String transactionPin) {
		Customer customer = customer(userId);
		requireTransactionPin(customer, transactionPin);
		PaymentCard card = lockedCustomerCard(customer, requestNumber);
		if (card.getStatus() != CardStatus.ACTIVE) {
			throw new CardException("Only an active card can be blocked.");
		}
		LocalDateTime now = LocalDateTime.now();
		card.block("Blocked by customer", now);
		cardRepository.save(card);
		notify(card, "Card blocked", "Your card " + masked(card) + " has been blocked as requested.");
		audit(actorUsername, "CARD_BLOCKED_BY_CUSTOMER", card, CardStatus.ACTIVE, CardStatus.BLOCKED,
				"Blocked by customer", now);
	}

	@Override
	@Transactional
	public void reactivateByCustomer(Long userId, String actorUsername, String requestNumber, String transactionPin) {
		Customer customer = customer(userId);
		requireTransactionPin(customer, transactionPin);
		PaymentCard card = lockedCustomerCard(customer, requestNumber);
		if (card.getStatus() != CardStatus.BLOCKED || !"Blocked by customer".equals(card.getBlockReason())) {
			throw new CardException("A card blocked by bank staff must be reviewed by bank staff before reactivation.");
		}
		LocalDateTime now = LocalDateTime.now();
		card.activate(now);
		cardRepository.save(card);
		notify(card, "Card reactivated", "Your card " + masked(card) + " is active again.");
		audit(actorUsername, "CARD_REACTIVATED_BY_CUSTOMER", card, CardStatus.BLOCKED, CardStatus.ACTIVE,
				"Customer reactivated the card.", now);
	}

	@Override
	public List<CardView> getCardsForReview(CardStatus status) {
		CardStatus selected = status == null ? CardStatus.PENDING_REVIEW : status;
		return cardRepository.findByStatusOrderByRequestedAtAsc(selected).stream().map(this::view).toList();
	}

	@Override
	public CardView getCardForStaff(String requestNumber) {
		return view(cardRepository.findByRequestNumber(requestNumber)
				.orElseThrow(() -> new CardException("Card request was not found.")));
	}

	@Override
	@Transactional
	public void approve(String actorUsername, String requestNumber, String note) {
		PaymentCard card = lockedCard(requestNumber);
		requireStatus(card, CardStatus.PENDING_REVIEW, "Only a pending card request can be approved.");
		if (card.getBankAccount().getAccountStatus() != AccountStatus.ACTIVE) {
			throw new CardException("The linked bank account is not active.");
		}
		LocalDateTime now = LocalDateTime.now();
		card.approve(actor(actorUsername), cardNumber(card.getCardType()), expiryDate(), optional(note), now);
		cardRepository.saveAndFlush(card);
		notify(card, "Card request approved",
				"Your " + card.getCardType().getDisplayName() + " " + masked(card)
						+ " is ready. Open Cards and activate it using your transaction PIN.");
		audit(actorUsername, "CARD_REQUEST_APPROVED", card, CardStatus.PENDING_REVIEW, CardStatus.INACTIVE,
				"Card issued and awaiting customer activation.", now);
	}

	@Override
	@Transactional
	public void reject(String actorUsername, String requestNumber, String reason) {
		PaymentCard card = lockedCard(requestNumber);
		requireStatus(card, CardStatus.PENDING_REVIEW, "Only a pending card request can be rejected.");
		String cleanReason = requiredReason(reason);
		LocalDateTime now = LocalDateTime.now();
		card.reject(actor(actorUsername), cleanReason, now);
		cardRepository.save(card);
		notify(card, "Card request update",
				"Your card request " + card.getRequestNumber() + " was not approved. Reason: " + cleanReason);
		audit(actorUsername, "CARD_REQUEST_REJECTED", card, CardStatus.PENDING_REVIEW, CardStatus.REJECTED,
				cleanReason, now);
	}

	@Override
	@Transactional
	public void blockByStaff(String actorUsername, String requestNumber, String reason) {
		PaymentCard card = lockedCard(requestNumber);
		if (card.getStatus() != CardStatus.ACTIVE && card.getStatus() != CardStatus.INACTIVE) {
			throw new CardException("Only an active or inactive issued card can be blocked.");
		}
		String cleanReason = requiredReason(reason);
		CardStatus previous = card.getStatus();
		LocalDateTime now = LocalDateTime.now();
		card.block("Bank staff: " + cleanReason, now);
		cardRepository.save(card);
		notify(card, "Card blocked by DigiBank", "Your card " + masked(card) + " was blocked. Reason: " + cleanReason);
		audit(actorUsername, "CARD_BLOCKED_BY_STAFF", card, previous, CardStatus.BLOCKED, cleanReason, now);
	}

	@Override
	@Transactional
	public void reactivateByStaff(String actorUsername, String requestNumber, String note) {
		PaymentCard card = lockedCard(requestNumber);
		requireStatus(card, CardStatus.BLOCKED, "Only a blocked card can be reactivated.");
		if (card.getExpiryDate() == null || card.getExpiryDate().isBefore(LocalDate.now())) {
			throw new CardException("This card has expired and cannot be reactivated.");
		}
		LocalDateTime now = LocalDateTime.now();
		card.activate(now);
		cardRepository.save(card);
		notify(card, "Card reactivated by DigiBank", "Your card " + masked(card) + " is active again.");
		audit(actorUsername, "CARD_REACTIVATED_BY_STAFF", card, CardStatus.BLOCKED, CardStatus.ACTIVE,
				optional(note), now);
	}

	private Customer customer(Long userId) {
		return customerRepository.findByUserId(userId)
				.orElseThrow(() -> new CardException("Customer profile was not found."));
	}

	private List<BankAccount> eligibleAccounts(Customer customer) {
		return accountRepository.findByCustomerId(customer.getId()).stream()
				.filter(account -> account.getAccountStatus() == AccountStatus.ACTIVE)
				.filter(account -> account.getCurrencyCode() == CurrencyCode.LKR)
				.toList();
	}

	private PaymentCard customerCard(Long userId, String requestNumber) {
		return cardRepository.findByRequestNumberAndCustomerUserId(requestNumber, userId)
				.orElseThrow(() -> new CardException("Card request was not found."));
	}

	private PaymentCard lockedCustomerCard(Customer customer, String requestNumber) {
		PaymentCard card = lockedCard(requestNumber);
		if (!card.getCustomer().getId().equals(customer.getId())) {
			throw new CardException("Card request was not found.");
		}
		return card;
	}

	private PaymentCard lockedCard(String requestNumber) {
		return cardRepository.findByRequestNumberForUpdate(requestNumber)
				.orElseThrow(() -> new CardException("Card request was not found."));
	}

	private void requireTransactionPin(Customer customer, String pin) {
		String value = clean(pin);
		if (value == null || !value.matches("\\d{4}")) {
			throw new CardException("Enter your 4-digit transaction PIN.");
		}
		String hash = customer.getUser().getTransactionPinHash();
		if (hash == null || !passwordEncoder.matches(value, hash)) {
			throw new CardException("The transaction PIN is incorrect.");
		}
	}

	private void requireStatus(PaymentCard card, CardStatus expected, String message) {
		if (card.getStatus() != expected) {
			throw new CardException(message);
		}
	}

	private String requestNumber() {
		for (int i = 0; i < GENERATION_ATTEMPTS; i++) {
			String candidate = "CRD" + UUID.randomUUID().toString().replace("-", "").substring(0, 17).toUpperCase(Locale.ROOT);
			if (!cardRepository.existsByRequestNumber(candidate)) {
				return candidate;
			}
		}
		throw new CardException("A card request number could not be generated. Please try again.");
	}

	private String cardNumber(CardType type) {
		String prefix = type == CardType.DEBIT ? "4532" : "5425";
		for (int attempt = 0; attempt < GENERATION_ATTEMPTS; attempt++) {
			StringBuilder partial = new StringBuilder(prefix);
			while (partial.length() < 15) {
				partial.append(secureRandom.nextInt(10));
			}
			String candidate = partial.append(luhnCheckDigit(partial.toString())).toString();
			if (!cardRepository.existsByCardNumber(candidate)) {
				return candidate;
			}
		}
		throw new CardException("A card number could not be generated. Please try again.");
	}

	private int luhnCheckDigit(String partial) {
		int sum = 0;
		boolean doubleDigit = true;
		for (int i = partial.length() - 1; i >= 0; i--) {
			int digit = partial.charAt(i) - '0';
			if (doubleDigit) {
				digit *= 2;
				if (digit > 9) digit -= 9;
			}
			sum += digit;
			doubleDigit = !doubleDigit;
		}
		return (10 - (sum % 10)) % 10;
	}

	private LocalDate expiryDate() {
		return LocalDate.now().plusYears(5).withDayOfMonth(1).plusMonths(1).minusDays(1);
	}

	private String cardholderName(Customer customer) {
		String name = (customer.getFirstName() + " " + customer.getLastName()).trim().toUpperCase(Locale.ROOT);
		return name.length() <= 100 ? name : name.substring(0, 100);
	}

	private CardView view(PaymentCard card) {
		Customer customer = card.getCustomer();
		return new CardView(card.getRequestNumber(), customer.getFirstName() + " " + customer.getLastName(),
				customer.getCustomerNumber(), dataMasker.maskAccountNumber(card.getBankAccount().getAccountNumber()),
				card.getCardType(), card.getStatus(), card.getCardholderName(), masked(card), card.getExpiryDate(),
				card.getRequestedAt(), card.getReviewedBy(), card.getReviewedAt(), card.getReviewNote(),
				card.getActivatedAt(), card.getBlockedAt(), card.getBlockReason());
	}

	private String masked(PaymentCard card) {
		String number = card.getCardNumber();
		return number == null ? "Not issued" : "•••• •••• •••• " + number.substring(number.length() - 4);
	}

	private void notify(PaymentCard card, String title, String message) {
		notificationRepository.save(new CustomerNotification(card.getCustomer().getUser(),
				NotificationType.ACCOUNT_NOTICE, title, message, card.getRequestNumber()));
	}

	private void audit(String actorUsername, String action, PaymentCard card, CardStatus previous, CardStatus next,
			String reason, LocalDateTime when) {
		auditLogRepository.save(new AuditLog(actor(actorUsername), action, "PAYMENT_CARD", card.getRequestNumber(),
				previous == null ? null : previous.name(), next == null ? null : next.name(), bounded(reason), when));
	}

	private String requiredReason(String reason) {
		String value = clean(reason);
		if (value == null || value.length() < 5) {
			throw new CardException("Enter a reason of at least 5 characters.");
		}
		if (value.length() > 200) {
			throw new CardException("Reason cannot exceed 200 characters.");
		}
		return value;
	}

	private String optional(String value) {
		String cleaned = clean(value);
		if (cleaned != null && cleaned.length() > 200) {
			throw new CardException("Note cannot exceed 200 characters.");
		}
		return cleaned;
	}

	private String actor(String username) {
		String value = clean(username);
		return value == null ? "system" : value;
	}

	private String bounded(String value) {
		String cleanValue = clean(value);
		return cleanValue == null || cleanValue.length() <= 255 ? cleanValue : cleanValue.substring(0, 255);
	}

	private String clean(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
