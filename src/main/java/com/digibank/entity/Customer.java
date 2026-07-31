package com.digibank.entity;

import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
		name = "customers",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_customers_user_id", columnNames = "user_id"),
				@UniqueConstraint(name = "uk_customers_customer_number", columnNames = "customer_number"),
				@UniqueConstraint(name = "uk_customers_identity_number", columnNames = "identity_number")
		}
)
public class Customer extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "customer_number", nullable = false, length = 30)
	private String customerNumber;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(name = "date_of_birth", nullable = false)
	private LocalDate dateOfBirth;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender", nullable = false, length = 30)
	private Gender gender;

	@Enumerated(EnumType.STRING)
	@Column(name = "identity_type", nullable = false, length = 30)
	private IdentityType identityType;

	@Column(name = "identity_number", nullable = false, length = 80)
	private String identityNumber;

	@Column(name = "nationality", length = 100)
	private String nationality;

	@Column(name = "mobile_number", nullable = false, length = 30)
	private String mobileNumber;

	@Column(name = "alternative_phone", length = 30)
	private String alternativePhone;

	@Column(name = "address_line_1", nullable = false, length = 150)
	private String addressLine1;

	@Column(name = "address_line_2", length = 150)
	private String addressLine2;

	@Column(name = "city", nullable = false, length = 100)
	private String city;

	@Column(name = "district", length = 100)
	private String district;

	@Column(name = "province", length = 100)
	private String province;

	@Column(name = "postal_code", length = 20)
	private String postalCode;

	@Column(name = "country", nullable = false, length = 100)
	private String country;

	@Column(name = "profile_image_path", length = 255)
	private String profileImagePath;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 40)
	private CustomerStatus status;

	@Column(name = "terms_accepted_at")
	private LocalDateTime termsAcceptedAt;

	@Column(name = "privacy_accepted_at")
	private LocalDateTime privacyAcceptedAt;

	@Column(name = "deactivated_at")
	private LocalDateTime deactivatedAt;

	@OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
	private List<BankAccount> bankAccounts = new ArrayList<>();

	protected Customer() {
	}

	public Customer(User user, String customerNumber, String firstName, String lastName,
			LocalDate dateOfBirth, Gender gender, IdentityType identityType,
			String identityNumber, String mobileNumber, String addressLine1, String city) {
		this.user = user;
		this.customerNumber = customerNumber;
		this.firstName = firstName;
		this.lastName = lastName;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.identityType = identityType;
		this.identityNumber = identityNumber;
		this.mobileNumber = mobileNumber;
		this.addressLine1 = addressLine1;
		this.city = city;
		this.status = CustomerStatus.PENDING_VERIFICATION;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getCustomerNumber() {
		return customerNumber;
	}

	public void setCustomerNumber(String customerNumber) {
		this.customerNumber = customerNumber;
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

	public String getProfileImagePath() {
		return profileImagePath;
	}

	public void setProfileImagePath(String profileImagePath) {
		this.profileImagePath = profileImagePath;
	}

	public CustomerStatus getStatus() {
		return status;
	}

	public void setStatus(CustomerStatus status) {
		this.status = status;
	}

	public LocalDateTime getTermsAcceptedAt() {
		return termsAcceptedAt;
	}

	public void setTermsAcceptedAt(LocalDateTime termsAcceptedAt) {
		this.termsAcceptedAt = termsAcceptedAt;
	}

	public LocalDateTime getPrivacyAcceptedAt() {
		return privacyAcceptedAt;
	}

	public void setPrivacyAcceptedAt(LocalDateTime privacyAcceptedAt) {
		this.privacyAcceptedAt = privacyAcceptedAt;
	}

	public LocalDateTime getDeactivatedAt() {
		return deactivatedAt;
	}

	public void setDeactivatedAt(LocalDateTime deactivatedAt) {
		this.deactivatedAt = deactivatedAt;
	}

	public List<BankAccount> getBankAccounts() {
		return bankAccounts;
	}

	public void addBankAccount(BankAccount bankAccount) {
		if (bankAccount == null) {
			return;
		}
		if (!this.bankAccounts.contains(bankAccount)) {
			this.bankAccounts.add(bankAccount);
		}
		if (bankAccount.getCustomer() != this) {
			bankAccount.setCustomer(this);
		}
	}

	public String getFullName() {
		String safeFirstName = firstName == null ? "" : firstName.trim();
		String safeLastName = lastName == null ? "" : lastName.trim();
		return (safeFirstName + " " + safeLastName).trim();
	}
}
