package com.digibank.entity;

import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
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
@Table(
		name = "bank_accounts",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_bank_accounts_account_number", columnNames = "account_number")
		}
)
public class BankAccount extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Column(name = "account_number", nullable = false, length = 30)
	private String accountNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_type", nullable = false, length = 30)
	private AccountType accountType;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_status", nullable = false, length = 40)
	private AccountStatus accountStatus;

	@Column(name = "branch_code", nullable = false, length = 20)
	private String branchCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "currency_code", nullable = false, length = 3)
	private CurrencyCode currencyCode;

	@Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
	private BigDecimal availableBalance;

	@Column(name = "current_balance", nullable = false, precision = 19, scale = 2)
	private BigDecimal currentBalance;

	@Column(name = "opened_at")
	private LocalDateTime openedAt;

	@Column(name = "frozen_at")
	private LocalDateTime frozenAt;

	@Column(name = "closed_at")
	private LocalDateTime closedAt;

	@Column(name = "closure_reason", length = 255)
	private String closureReason;

	protected BankAccount() {
	}

	public BankAccount(Customer customer, String accountNumber, AccountType accountType, String branchCode) {
		this.customer = customer;
		this.accountNumber = accountNumber;
		this.accountType = accountType;
		this.branchCode = branchCode;
		this.accountStatus = AccountStatus.PENDING_ACTIVATION;
		this.currencyCode = CurrencyCode.LKR;
		this.availableBalance = BigDecimal.ZERO;
		this.currentBalance = BigDecimal.ZERO;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}

	public String getBranchCode() {
		return branchCode;
	}

	public void setBranchCode(String branchCode) {
		this.branchCode = branchCode;
	}

	public CurrencyCode getCurrencyCode() {
		return currencyCode;
	}

	public void setCurrencyCode(CurrencyCode currencyCode) {
		this.currencyCode = currencyCode;
	}

	public BigDecimal getAvailableBalance() {
		return availableBalance;
	}

	public void setAvailableBalance(BigDecimal availableBalance) {
		this.availableBalance = availableBalance;
	}

	public BigDecimal getCurrentBalance() {
		return currentBalance;
	}

	public void setCurrentBalance(BigDecimal currentBalance) {
		this.currentBalance = currentBalance;
	}

	public LocalDateTime getOpenedAt() {
		return openedAt;
	}

	public void setOpenedAt(LocalDateTime openedAt) {
		this.openedAt = openedAt;
	}

	public LocalDateTime getFrozenAt() {
		return frozenAt;
	}

	public void setFrozenAt(LocalDateTime frozenAt) {
		this.frozenAt = frozenAt;
	}

	public LocalDateTime getClosedAt() {
		return closedAt;
	}

	public void setClosedAt(LocalDateTime closedAt) {
		this.closedAt = closedAt;
	}

	public String getClosureReason() {
		return closureReason;
	}

	public void setClosureReason(String closureReason) {
		this.closureReason = closureReason;
	}
}
