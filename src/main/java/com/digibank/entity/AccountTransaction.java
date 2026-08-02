package com.digibank.entity;

import com.digibank.enums.AccountTransactionType;
import com.digibank.enums.TransactionDirection;
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
@Table(name = "account_transactions", uniqueConstraints = {
		@UniqueConstraint(name = "uk_account_transactions_transfer_account_direction",
				columnNames = {"fund_transfer_id", "account_id", "direction"})
})
public class AccountTransaction extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private BankAccount account;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fund_transfer_id")
	private FundTransfer fundTransfer;

	@Column(name = "reference_number", nullable = false, length = 32)
	private String referenceNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "direction", nullable = false, length = 10)
	private TransactionDirection direction;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false, length = 30)
	private AccountTransactionType transactionType;

	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "balance_after", precision = 19, scale = 2)
	private BigDecimal balanceAfter;

	@Column(name = "counterparty_name", nullable = false, length = 160)
	private String counterpartyName;

	@Column(name = "counterparty_account_masked", nullable = false, length = 34)
	private String counterpartyAccountMasked;

	@Column(name = "description", length = 140)
	private String description;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	protected AccountTransaction() {
	}

	public AccountTransaction(BankAccount account, FundTransfer fundTransfer, String referenceNumber,
			TransactionDirection direction, BigDecimal amount, BigDecimal balanceAfter, String counterpartyName,
			String counterpartyAccountMasked, String description, LocalDateTime occurredAt) {
		this(account, fundTransfer, referenceNumber, direction, AccountTransactionType.FUND_TRANSFER, amount,
				balanceAfter, counterpartyName, counterpartyAccountMasked, description, occurredAt);
	}

	public AccountTransaction(BankAccount account, String referenceNumber, TransactionDirection direction,
			AccountTransactionType transactionType, BigDecimal amount, BigDecimal balanceAfter, String counterpartyName,
			String counterpartyAccountMasked, String description, LocalDateTime occurredAt) {
		this(account, null, referenceNumber, direction, transactionType, amount, balanceAfter, counterpartyName,
				counterpartyAccountMasked, description, occurredAt);
	}

	private AccountTransaction(BankAccount account, FundTransfer fundTransfer, String referenceNumber,
			TransactionDirection direction, AccountTransactionType transactionType, BigDecimal amount,
			BigDecimal balanceAfter, String counterpartyName, String counterpartyAccountMasked, String description,
			LocalDateTime occurredAt) {
		this.account = account;
		this.fundTransfer = fundTransfer;
		this.referenceNumber = referenceNumber;
		this.direction = direction;
		this.transactionType = transactionType;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.counterpartyName = counterpartyName;
		this.counterpartyAccountMasked = counterpartyAccountMasked;
		this.description = description;
		this.occurredAt = occurredAt;
	}

	public BankAccount getAccount() { return account; }
	public FundTransfer getFundTransfer() { return fundTransfer; }
	public String getReferenceNumber() { return referenceNumber; }
	public TransactionDirection getDirection() { return direction; }
	public AccountTransactionType getTransactionType() { return transactionType; }
	public BigDecimal getAmount() { return amount; }
	public BigDecimal getBalanceAfter() { return balanceAfter; }
	public String getCounterpartyName() { return counterpartyName; }
	public String getCounterpartyAccountMasked() { return counterpartyAccountMasked; }
	public String getDescription() { return description; }
	public LocalDateTime getOccurredAt() { return occurredAt; }
}
