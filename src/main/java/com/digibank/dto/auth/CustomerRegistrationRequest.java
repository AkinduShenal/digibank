package com.digibank.dto.auth;

import com.digibank.enums.AccountType;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import com.digibank.validation.annotation.Adult;
import com.digibank.validation.annotation.MatchingFields;
import com.digibank.validation.annotation.ValidIdentityDetails;
import com.digibank.validation.annotation.ValidInitialDeposit;
import com.digibank.validation.annotation.ValidTransactionPin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@ValidIdentityDetails
@ValidInitialDeposit
@ValidTransactionPin
@MatchingFields(first = "password", second = "confirmPassword", message = "Passwords do not match.")
public class CustomerRegistrationRequest {

	@NotBlank(message = "First name is required.")
	@Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters.")
	@Pattern(regexp = "^[A-Za-z][A-Za-z '\\-]*$", message = "First name can contain only letters, spaces, apostrophes and hyphens.")
	private String firstName;

	@NotBlank(message = "Last name is required.")
	@Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters.")
	@Pattern(regexp = "^[A-Za-z][A-Za-z '\\-]*$", message = "Last name can contain only letters, spaces, apostrophes and hyphens.")
	private String lastName;

	@NotBlank(message = "Email is required.")
	@Email(message = "Enter a valid email address.")
	@Size(max = 120, message = "Email must not exceed 120 characters.")
	private String email;

	@NotBlank(message = "Mobile number is required.")
	@Pattern(regexp = "^(07\\d{8}|\\+947\\d{8})$", message = "Mobile number must use 07XXXXXXXX or +947XXXXXXXX format.")
	private String mobileNumber;

	@Pattern(regexp = "^(|07\\d{8}|\\+947\\d{8})$", message = "Alternative phone must use 07XXXXXXXX or +947XXXXXXXX format.")
	private String alternativePhone;

	@NotNull(message = "Date of birth is required.")
	@Past(message = "Date of birth must be in the past.")
	@Adult
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate dateOfBirth;

	@NotNull(message = "Gender is required.")
	private Gender gender;

	@NotNull(message = "Identity type is required.")
	private IdentityType identityType;

	@NotBlank(message = "Identity number is required.")
	@Size(max = 15, message = "Identity number must not exceed 15 characters.")
	private String identityNumber;

	@Size(max = 100, message = "Nationality must not exceed 100 characters.")
	private String nationality;

	@NotBlank(message = "Address line 1 is required.")
	@Size(max = 150, message = "Address line 1 must not exceed 150 characters.")
	private String addressLine1;

	@Size(max = 150, message = "Address line 2 must not exceed 150 characters.")
	private String addressLine2;

	@NotBlank(message = "City is required.")
	@Size(max = 100, message = "City must not exceed 100 characters.")
	private String city;

	@Size(max = 100, message = "District must not exceed 100 characters.")
	private String district;

	@Size(max = 100, message = "Province must not exceed 100 characters.")
	private String province;

	@Size(max = 20, message = "Postal code must not exceed 20 characters.")
	private String postalCode;

	@NotBlank(message = "Country is required.")
	@Size(max = 100, message = "Country must not exceed 100 characters.")
	private String country;

	@NotNull(message = "Account type is required.")
	private AccountType accountType;

	@NotBlank(message = "Branch code is required.")
	@Size(max = 20, message = "Branch code must not exceed 20 characters.")
	private String branchCode;

	@NotNull(message = "Initial deposit is required.")
	@DecimalMin(value = "0.00", message = "Initial deposit cannot be negative.")
	@Digits(integer = 17, fraction = 2, message = "Initial deposit can have a maximum of two decimal places.")
	private BigDecimal initialDeposit;

	@NotBlank(message = "Username is required.")
	@Size(min = 5, max = 30, message = "Username must be between 5 and 30 characters.")
	@Pattern(regexp = "^[A-Za-z][A-Za-z0-9._]*$", message = "Username must start with a letter and use only letters, numbers, dots and underscores.")
	private String username;

	@NotBlank(message = "Password is required.")
	@Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
	@Pattern(
			regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
			message = "Password must include uppercase, lowercase, digit and special character."
	)
	private String password;

	@NotBlank(message = "Confirm password is required.")
	private String confirmPassword;

	@NotBlank(message = "Transaction PIN is required.")
	@Pattern(regexp = "^\\d{4}$", message = "Transaction PIN must be exactly 4 digits.")
	private String transactionPin;

	@NotBlank(message = "Confirm transaction PIN is required.")
	@Pattern(regexp = "^\\d{4}$", message = "Confirm transaction PIN must be exactly 4 digits.")
	private String confirmTransactionPin;

	@AssertTrue(message = "You must accept the terms and conditions.")
	private boolean termsAccepted;

	@AssertTrue(message = "You must accept the privacy policy.")
	private boolean privacyAccepted;

	public CustomerRegistrationRequest() {
	}

	public CustomerRegistrationRequest(String firstName, String lastName, String email, String mobileNumber,
			LocalDate dateOfBirth, Gender gender, IdentityType identityType, String identityNumber,
			String addressLine1, String city, String country, AccountType accountType, String branchCode,
			BigDecimal initialDeposit, String username, String password, String confirmPassword,
			String transactionPin, String confirmTransactionPin, boolean termsAccepted, boolean privacyAccepted) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.mobileNumber = mobileNumber;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.identityType = identityType;
		this.identityNumber = identityNumber;
		this.addressLine1 = addressLine1;
		this.city = city;
		this.country = country;
		this.accountType = accountType;
		this.branchCode = branchCode;
		this.initialDeposit = initialDeposit;
		this.username = username;
		this.password = password;
		this.confirmPassword = confirmPassword;
		this.transactionPin = transactionPin;
		this.confirmTransactionPin = confirmTransactionPin;
		this.termsAccepted = termsAccepted;
		this.privacyAccepted = privacyAccepted;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public String getAlternativePhone() {
		return alternativePhone;
	}

	public void setAlternativePhone(String alternativePhone) {
		this.alternativePhone = alternativePhone;
	}

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public IdentityType getIdentityType() {
		return identityType;
	}

	public void setIdentityType(IdentityType identityType) {
		this.identityType = identityType;
	}

	public String getIdentityNumber() {
		return identityNumber;
	}

	public void setIdentityNumber(String identityNumber) {
		this.identityNumber = identityNumber;
	}

	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public String getAddressLine1() {
		return addressLine1;
	}

	public void setAddressLine1(String addressLine1) {
		this.addressLine1 = addressLine1;
	}

	public String getAddressLine2() {
		return addressLine2;
	}

	public void setAddressLine2(String addressLine2) {
		this.addressLine2 = addressLine2;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}

	public String getBranchCode() {
		return branchCode;
	}

	public void setBranchCode(String branchCode) {
		this.branchCode = branchCode;
	}

	public BigDecimal getInitialDeposit() {
		return initialDeposit;
	}

	public void setInitialDeposit(BigDecimal initialDeposit) {
		this.initialDeposit = initialDeposit;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public String getTransactionPin() {
		return transactionPin;
	}

	public void setTransactionPin(String transactionPin) {
		this.transactionPin = transactionPin;
	}

	public String getConfirmTransactionPin() {
		return confirmTransactionPin;
	}

	public void setConfirmTransactionPin(String confirmTransactionPin) {
		this.confirmTransactionPin = confirmTransactionPin;
	}

	public boolean isTermsAccepted() {
		return termsAccepted;
	}

	public void setTermsAccepted(boolean termsAccepted) {
		this.termsAccepted = termsAccepted;
	}

	public boolean isPrivacyAccepted() {
		return privacyAccepted;
	}

	public void setPrivacyAccepted(boolean privacyAccepted) {
		this.privacyAccepted = privacyAccepted;
	}
}
