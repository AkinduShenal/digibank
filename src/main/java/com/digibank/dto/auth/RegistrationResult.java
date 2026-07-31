package com.digibank.dto.auth;

import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CurrencyCode;

public class RegistrationResult {

	private final String customerNumber;
	private final String accountNumber;
	private final AccountType accountType;
	private final CurrencyCode currency;
	private final AccountStatus accountStatus;
	private final String customerFullName;

	public RegistrationResult(String customerNumber, String accountNumber, AccountType accountType,
			CurrencyCode currency, AccountStatus accountStatus, String customerFullName) {
		this.customerNumber = customerNumber;
		this.accountNumber = accountNumber;
		this.accountType = accountType;
		this.currency = currency;
		this.accountStatus = accountStatus;
		this.customerFullName = customerFullName;
	}

	public String getCustomerNumber() {
		return customerNumber;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public CurrencyCode getCurrency() {
		return currency;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public String getCustomerFullName() {
		return customerFullName;
	}

	public String getMaskedCustomerNumber() {
		if (customerNumber == null || customerNumber.length() <= 7) {
			return "******";
		}
		return customerNumber.substring(0, 7) + "***";
	}

	public String getMaskedAccountNumber() {
		if (accountNumber == null || accountNumber.length() <= 4) {
			return "********";
		}
		return "********" + accountNumber.substring(accountNumber.length() - 4);
	}
}
