package com.digibank.dto.staff;

import com.digibank.dto.customer.AccountSummaryView;
import com.digibank.enums.CustomerStatus;

import java.time.LocalDateTime;
import java.util.List;

public class StaffCustomerView {

	private final String customerNumber;
	private final String fullName;
	private final String email;
	private final String mobileNumber;
	private final CustomerStatus customerStatus;
	private final boolean userEnabled;
	private final LocalDateTime joinedAt;
	private final List<AccountSummaryView> accounts;

	public StaffCustomerView(String customerNumber, String fullName, String email, String mobileNumber,
			CustomerStatus customerStatus, boolean userEnabled, LocalDateTime joinedAt,
			List<AccountSummaryView> accounts) {
		this.customerNumber = customerNumber;
		this.fullName = fullName;
		this.email = email;
		this.mobileNumber = mobileNumber;
		this.customerStatus = customerStatus;
		this.userEnabled = userEnabled;
		this.joinedAt = joinedAt;
		this.accounts = accounts;
	}

	public String getCustomerNumber() {
		return customerNumber;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public CustomerStatus getCustomerStatus() {
		return customerStatus;
	}

	public boolean isUserEnabled() {
		return userEnabled;
	}

	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}

	public List<AccountSummaryView> getAccounts() {
		return accounts;
	}
}
