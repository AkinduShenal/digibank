package com.digibank.dto.customer;

import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CustomerProfileView {

	private final String fullName;
	private final String customerNumber;
	private final String maskedIdentityNumber;
	private final IdentityType identityType;
	private final LocalDate dateOfBirth;
	private final int age;
	private final Gender gender;
	private final String nationality;
	private final String email;
	private final String mobileNumber;
	private final String alternativePhone;
	private final String fullAddress;
	private final CustomerStatus customerStatus;
	private final LocalDateTime joinedAt;
	private final String initials;
	private final String profileImagePath;
	private final List<AccountSummaryView> accounts;

	public CustomerProfileView(String fullName, String customerNumber, String maskedIdentityNumber,
			IdentityType identityType, LocalDate dateOfBirth, int age, Gender gender, String nationality,
			String email, String mobileNumber, String alternativePhone, String fullAddress,
			CustomerStatus customerStatus, LocalDateTime joinedAt, String initials, String profileImagePath,
			List<AccountSummaryView> accounts) {
		this.fullName = fullName;
		this.customerNumber = customerNumber;
		this.maskedIdentityNumber = maskedIdentityNumber;
		this.identityType = identityType;
		this.dateOfBirth = dateOfBirth;
		this.age = age;
		this.gender = gender;
		this.nationality = nationality;
		this.email = email;
		this.mobileNumber = mobileNumber;
		this.alternativePhone = alternativePhone;
		this.fullAddress = fullAddress;
		this.customerStatus = customerStatus;
		this.joinedAt = joinedAt;
		this.initials = initials;
		this.profileImagePath = profileImagePath;
		this.accounts = accounts;
	}

	public String getFullName() {
		return fullName;
	}

	public String getCustomerNumber() {
		return customerNumber;
	}

	public String getMaskedIdentityNumber() {
		return maskedIdentityNumber;
	}

	public IdentityType getIdentityType() {
		return identityType;
	}

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public int getAge() {
		return age;
	}

	public Gender getGender() {
		return gender;
	}

	public String getNationality() {
		return nationality;
	}

	public String getEmail() {
		return email;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public String getAlternativePhone() {
		return alternativePhone;
	}

	public String getFullAddress() {
		return fullAddress;
	}

	public CustomerStatus getCustomerStatus() {
		return customerStatus;
	}

	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}

	public String getInitials() {
		return initials;
	}

	public String getProfileImagePath() {
		return profileImagePath;
	}

	public List<AccountSummaryView> getAccounts() {
		return accounts;
	}
}
