package com.digibank.entity;

import com.digibank.enums.CardStatus;
import com.digibank.enums.CardType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "payment_cards", uniqueConstraints = {
		@UniqueConstraint(name = "uk_payment_cards_request_number", columnNames = "request_number"),
		@UniqueConstraint(name = "uk_payment_cards_card_number", columnNames = "card_number")
})
public class PaymentCard extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_account_id", nullable = false)
	private BankAccount bankAccount;

	@Column(name = "request_number", nullable = false, length = 32)
	private String requestNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "card_type", nullable = false, length = 20)
	private CardType cardType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private CardStatus status;

	@Column(name = "cardholder_name", nullable = false, length = 100)
	private String cardholderName;

	@Column(name = "card_number", length = 255)
	private String encryptedCardNumber;

	@Column(name = "card_number_hash", length = 64)
	private String cardNumberHash;

	@Column(name = "card_last_four", length = 4)
	private String cardLastFour;

	@Column(name = "expiry_date")
	private LocalDate expiryDate;

	@Column(name = "spending_limit", nullable = false, precision = 19, scale = 2)
	private BigDecimal spendingLimit;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "reviewed_by", length = 120)
	private String reviewedBy;

	@Column(name = "reviewed_at")
	private LocalDateTime reviewedAt;

	@Column(name = "review_note", length = 255)
	private String reviewNote;

	@Column(name = "activated_at")
	private LocalDateTime activatedAt;

	@Column(name = "blocked_at")
	private LocalDateTime blockedAt;

	@Column(name = "lost_stolen_at")
	private LocalDateTime lostStolenAt;

	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

	@Column(name = "cancellation_reason", length = 255)
	private String cancellationReason;

	@Column(name = "block_reason", length = 255)
	private String blockReason;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected PaymentCard() {
	}

	public PaymentCard(Customer customer, BankAccount bankAccount, String requestNumber, CardType cardType,
			String cardholderName, LocalDateTime requestedAt) {
		this.customer = customer;
		this.bankAccount = bankAccount;
		this.requestNumber = requestNumber;
		this.cardType = cardType;
		this.cardholderName = cardholderName;
		this.requestedAt = requestedAt;
		this.status = CardStatus.PENDING_REVIEW;
		this.spendingLimit = cardType == CardType.CREDIT ? new BigDecimal("500000.00") : new BigDecimal("250000.00");
	}

	public void approve(String reviewer, String encryptedNumber, String numberHash, String lastFour,
			LocalDate expiry, String note, LocalDateTime when) {
		this.encryptedCardNumber = encryptedNumber;
		this.cardNumberHash = numberHash;
		this.cardLastFour = lastFour;
		this.expiryDate = expiry;
		this.reviewedBy = reviewer;
		this.reviewNote = note;
		this.reviewedAt = when;
		this.status = CardStatus.INACTIVE;
	}

	public void reject(String reviewer, String reason, LocalDateTime when) {
		this.reviewedBy = reviewer;
		this.reviewNote = reason;
		this.reviewedAt = when;
		this.status = CardStatus.REJECTED;
	}

	public void activate(LocalDateTime when) {
		this.status = CardStatus.ACTIVE;
		this.activatedAt = when;
		this.blockedAt = null;
		this.blockReason = null;
	}

	public void block(String reason, LocalDateTime when) {
		this.status = CardStatus.BLOCKED;
		this.blockedAt = when;
		this.blockReason = reason;
	}

	public void changeSpendingLimit(BigDecimal limit) { this.spendingLimit = limit; }

	public void reportLostOrStolen(String reason, LocalDateTime when) {
		this.status = CardStatus.LOST_STOLEN;
		this.blockedAt = when;
		this.lostStolenAt = when;
		this.blockReason = reason;
	}

	public void cancel(String reason, LocalDateTime when) {
		this.status = CardStatus.CANCELLED;
		this.cancelledAt = when;
		this.cancellationReason = reason;
	}

	public void expire(LocalDateTime when) {
		this.status = CardStatus.EXPIRED;
		this.blockedAt = when;
		this.blockReason = "Card expired";
	}

	public void secureLegacyNumber(String encryptedNumber, String numberHash, String lastFour) {
		this.encryptedCardNumber = encryptedNumber;
		this.cardNumberHash = numberHash;
		this.cardLastFour = lastFour;
	}

	public Customer getCustomer() { return customer; }
	public BankAccount getBankAccount() { return bankAccount; }
	public String getRequestNumber() { return requestNumber; }
	public CardType getCardType() { return cardType; }
	public CardStatus getStatus() { return status; }
	public String getCardholderName() { return cardholderName; }
	public String getEncryptedCardNumber() { return encryptedCardNumber; }
	public String getCardNumberHash() { return cardNumberHash; }
	public String getCardLastFour() { return cardLastFour; }
	public LocalDate getExpiryDate() { return expiryDate; }
	public BigDecimal getSpendingLimit() { return spendingLimit; }
	public LocalDateTime getRequestedAt() { return requestedAt; }
	public String getReviewedBy() { return reviewedBy; }
	public LocalDateTime getReviewedAt() { return reviewedAt; }
	public String getReviewNote() { return reviewNote; }
	public LocalDateTime getActivatedAt() { return activatedAt; }
	public LocalDateTime getBlockedAt() { return blockedAt; }
	public String getBlockReason() { return blockReason; }
	public LocalDateTime getLostStolenAt() { return lostStolenAt; }
	public LocalDateTime getCancelledAt() { return cancelledAt; }
	public String getCancellationReason() { return cancellationReason; }
	public long getVersion() { return version; }
}
