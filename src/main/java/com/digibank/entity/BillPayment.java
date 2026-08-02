package com.digibank.entity;

import com.digibank.enums.BillPaymentStatus;
import com.digibank.enums.BillerCategory;
import com.digibank.enums.BillerProvider;
import com.digibank.enums.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill_payments", uniqueConstraints = {
		@UniqueConstraint(name = "uk_bill_payments_reference", columnNames = "reference_number")
})
public class BillPayment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "source_account_id", nullable = false)
	private BankAccount sourceAccount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "saved_biller_id")
	private SavedBiller savedBiller;

	@Column(name = "reference_number", nullable = false, length = 32)
	private String referenceNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 30)
	private BillerCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, length = 40)
	private BillerProvider provider;

	@Column(name = "consumer_reference", nullable = false, length = 50)
	private String consumerReference;

	@Column(name = "biller_name_snapshot", nullable = false, length = 120)
	private String billerNameSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "currency_code", nullable = false, length = 3)
	private CurrencyCode currencyCode;

	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private BillPaymentStatus status;

	@Column(name = "source_balance_after", nullable = false, precision = 19, scale = 2)
	private BigDecimal sourceBalanceAfter;

	@Column(name = "description", length = 140)
	private String description;

	@Column(name = "paid_at", nullable = false)
	private LocalDateTime paidAt;

	protected BillPayment() {
	}

	public BillPayment(Customer customer, BankAccount sourceAccount, SavedBiller savedBiller, String referenceNumber,
			BillerProvider provider, String consumerReference, BigDecimal amount, BigDecimal sourceBalanceAfter,
			String description, LocalDateTime paidAt) {
		this.customer = customer;
		this.sourceAccount = sourceAccount;
		this.savedBiller = savedBiller;
		this.referenceNumber = referenceNumber;
		this.category = provider.getCategory();
		this.provider = provider;
		this.consumerReference = consumerReference;
		this.billerNameSnapshot = provider.getDisplayName();
		this.currencyCode = CurrencyCode.LKR;
		this.amount = amount;
		this.status = BillPaymentStatus.COMPLETED;
		this.sourceBalanceAfter = sourceBalanceAfter;
		this.description = description;
		this.paidAt = paidAt;
	}

	public Customer getCustomer() { return customer; }
	public BankAccount getSourceAccount() { return sourceAccount; }
	public SavedBiller getSavedBiller() { return savedBiller; }
	public String getReferenceNumber() { return referenceNumber; }
	public BillerCategory getCategory() { return category; }
	public BillerProvider getProvider() { return provider; }
	public String getConsumerReference() { return consumerReference; }
	public String getBillerNameSnapshot() { return billerNameSnapshot; }
	public CurrencyCode getCurrencyCode() { return currencyCode; }
	public BigDecimal getAmount() { return amount; }
	public BillPaymentStatus getStatus() { return status; }
	public BigDecimal getSourceBalanceAfter() { return sourceBalanceAfter; }
	public String getDescription() { return description; }
	public LocalDateTime getPaidAt() { return paidAt; }
}
