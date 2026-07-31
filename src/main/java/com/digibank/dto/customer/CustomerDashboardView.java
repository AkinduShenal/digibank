package com.digibank.dto.customer;

import java.time.LocalDateTime;

public class CustomerDashboardView {

	private final String fullName;
	private final String customerNumber;
	private final String initials;
	private final String profileImagePath;
	private final LocalDateTime lastLoginAt;
	private final AccountSummaryView primaryAccount;

	public CustomerDashboardView(String fullName, String customerNumber, String initials, String profileImagePath,
			LocalDateTime lastLoginAt, AccountSummaryView primaryAccount) {
		this.fullName = fullName;
		this.customerNumber = customerNumber;
		this.initials = initials;
		this.profileImagePath = profileImagePath;
		this.lastLoginAt = lastLoginAt;
		this.primaryAccount = primaryAccount;
	}

	public String getFullName() {
		return fullName;
	}

	public String getCustomerNumber() {
		return customerNumber;
	}

	public String getInitials() {
		return initials;
	}

	public String getProfileImagePath() {
		return profileImagePath;
	}

	public LocalDateTime getLastLoginAt() {
		return lastLoginAt;
	}

	public AccountSummaryView getPrimaryAccount() {
		return primaryAccount;
	}
}
