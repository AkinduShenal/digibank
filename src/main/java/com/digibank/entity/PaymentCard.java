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

	@Column(name = "card_number", length = 19)
	private String cardNumber;

	@Column(name = "expiry_date")
	private LocalDate expiryDate;

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
	}

	public void approve(String reviewer, String number, LocalDate expiry, String note, LocalDateTime when) {
		this.cardNumber = number;
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

	public Customer getCustomer() { return customer; }
	public BankAccount getBankAccount() { return bankAccount; }
	public String getRequestNumber() { return requestNumber; }
	public CardType getCardType() { return cardType; }
	public CardStatus getStatus() { return status; }
	public String getCardholderName() { return cardholderName; }
	public String getCardNumber() { return cardNumber; }
	public LocalDate getExpiryDate() { return expiryDate; }
	public LocalDateTime getRequestedAt() { return requestedAt; }
	public String getReviewedBy() { return reviewedBy; }
	public LocalDateTime getReviewedAt() { return reviewedAt; }
	public String getReviewNote() { return reviewNote; }
	public LocalDateTime getActivatedAt() { return activatedAt; }
	public LocalDateTime getBlockedAt() { return blockedAt; }
	public String getBlockReason() { return blockReason; }
	public long getVersion() { return version; }
}
