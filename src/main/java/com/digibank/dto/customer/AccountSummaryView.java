package com.digibank.dto.customer;

import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountSummaryView {

	private final String accountNumber;
	private final String maskedAccountNumber;
	private final AccountType accountType;
	private final AccountStatus accountStatus;
	private final CurrencyCode currencyCode;
	private final BigDecimal availableBalance;
	private final BigDecimal currentBalance;
	private final String branchCode;
	private final LocalDateTime openedAt;

	public AccountSummaryView(String accountNumber, String maskedAccountNumber, AccountType accountType,
			AccountStatus accountStatus, CurrencyCode currencyCode, BigDecimal availableBalance,
			BigDecimal currentBalance, String branchCode, LocalDateTime openedAt) {
		this.accountNumber = accountNumber;
		this.maskedAccountNumber = maskedAccountNumber;
		this.accountType = accountType;
		this.accountStatus = accountStatus;
		this.currencyCode = currencyCode;
		this.availableBalance = availableBalance;
		this.currentBalance = currentBalance;
		this.branchCode = branchCode;
		this.openedAt = openedAt;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public String getMaskedAccountNumber() {
		return maskedAccountNumber;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public CurrencyCode getCurrencyCode() {
		return currencyCode;
	}

	public BigDecimal getAvailableBalance() {
		return availableBalance;
	}

	public BigDecimal getCurrentBalance() {
		return currentBalance;
	}

	public String getBranchCode() {
		return branchCode;
	}

	public LocalDateTime getOpenedAt() {
		return openedAt;
	}
}
