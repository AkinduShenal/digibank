package com.digibank.entity;

import com.digibank.enums.CurrencyCode;
import com.digibank.enums.TransferStatus;
import com.digibank.enums.TransferType;
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
@Table(name = "fund_transfers", uniqueConstraints = {
		@UniqueConstraint(name = "uk_fund_transfers_reference", columnNames = "reference_number")
})
public class FundTransfer extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "source_account_id", nullable = false)
	private BankAccount sourceAccount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "beneficiary_id")
	private Beneficiary beneficiary;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "destination_account_id")
	private BankAccount destinationAccount;

	@Column(name = "reference_number", nullable = false, length = 32)
	private String referenceNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "transfer_type", nullable = false, length = 20)
	private TransferType transferType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private TransferStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "currency_code", nullable = false, length = 3)
	private CurrencyCode currencyCode;

	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "description", length = 140)
	private String description;

	@Column(name = "beneficiary_name_snapshot", nullable = false, length = 120)
	private String beneficiaryNameSnapshot;

	@Column(name = "destination_bank_snapshot", nullable = false, length = 120)
	private String destinationBankSnapshot;

	@Column(name = "destination_account_masked", nullable = false, length = 34)
	private String destinationAccountMasked;

	@Column(name = "source_balance_after", nullable = false, precision = 19, scale = 2)
	private BigDecimal sourceBalanceAfter;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "failure_reason", length = 255)
	private String failureReason;

	@Column(name = "reversal_reference", length = 32, unique = true)
	private String reversalReference;

	@Column(name = "reversal_reason", length = 255)
	private String reversalReason;

	@Column(name = "reversed_by", length = 30)
	private String reversedBy;

	@Column(name = "reversed_at")
	private LocalDateTime reversedAt;

	protected FundTransfer() {
	}

	public FundTransfer(Customer customer, BankAccount sourceAccount, Beneficiary beneficiary,
			String referenceNumber, TransferType transferType, BigDecimal amount, String description,
			String beneficiaryNameSnapshot, String destinationBankSnapshot, String destinationAccountMasked) {
		this.customer = customer;
		this.sourceAccount = sourceAccount;
		this.beneficiary = beneficiary;
		this.referenceNumber = referenceNumber;
		this.transferType = transferType;
		this.status = TransferStatus.PENDING;
		this.currencyCode = CurrencyCode.LKR;
		this.amount = amount;
		this.description = description;
		this.beneficiaryNameSnapshot = beneficiaryNameSnapshot;
		this.destinationBankSnapshot = destinationBankSnapshot;
		this.destinationAccountMasked = destinationAccountMasked;
		this.sourceBalanceAfter = sourceAccount.getAvailableBalance();
	}

	public Customer getCustomer() { return customer; }
	public BankAccount getSourceAccount() { return sourceAccount; }
	public Beneficiary getBeneficiary() { return beneficiary; }
	public BankAccount getDestinationAccount() { return destinationAccount; }
	public void setDestinationAccount(BankAccount destinationAccount) { this.destinationAccount = destinationAccount; }
	public String getReferenceNumber() { return referenceNumber; }
	public TransferType getTransferType() { return transferType; }
	public TransferStatus getStatus() { return status; }
	public void setStatus(TransferStatus status) { this.status = status; }
	public CurrencyCode getCurrencyCode() { return currencyCode; }
	public BigDecimal getAmount() { return amount; }
	public String getDescription() { return description; }
	public String getBeneficiaryNameSnapshot() { return beneficiaryNameSnapshot; }
	public String getDestinationBankSnapshot() { return destinationBankSnapshot; }
	public String getDestinationAccountMasked() { return destinationAccountMasked; }
	public BigDecimal getSourceBalanceAfter() { return sourceBalanceAfter; }
	public void setSourceBalanceAfter(BigDecimal sourceBalanceAfter) { this.sourceBalanceAfter = sourceBalanceAfter; }
	public LocalDateTime getCompletedAt() { return completedAt; }
	public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
	public String getFailureReason() { return failureReason; }
	public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
	public String getReversalReference() { return reversalReference; }
	public String getReversalReason() { return reversalReason; }
	public String getReversedBy() { return reversedBy; }
	public LocalDateTime getReversedAt() { return reversedAt; }
	public void reverse(String reversalReference, String reason, String actor, LocalDateTime when) {
		this.status = TransferStatus.REVERSED;
		this.reversalReference = reversalReference;
		this.reversalReason = reason;
		this.reversedBy = actor;
		this.reversedAt = when;
	}
}
