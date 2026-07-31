package com.digibank.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerProfileUpdateRequest {

	private static final String NAME_PATTERN = "^[A-Za-z][A-Za-z '\\-]*$";
	private static final String MOBILE_PATTERN = "^(07\\d{8}|\\+947\\d{8})$";

	@NotBlank(message = "First name is required.")
	@Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters.")
	@Pattern(regexp = NAME_PATTERN, message = "First name can contain letters, spaces, apostrophes and hyphens only.")
	private String firstName;

	@NotBlank(message = "Last name is required.")
	@Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters.")
	@Pattern(regexp = NAME_PATTERN, message = "Last name can contain letters, spaces, apostrophes and hyphens only.")
	private String lastName;

	@NotBlank(message = "Mobile number is required.")
	@Pattern(regexp = MOBILE_PATTERN, message = "Use a Sri Lankan mobile number such as 0712345678 or +94712345678.")
	private String mobileNumber;

	@Pattern(regexp = MOBILE_PATTERN, message = "Use a Sri Lankan mobile number such as 0712345678 or +94712345678.")
	private String alternativePhone;

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

	@Size(max = 100, message = "Nationality must not exceed 100 characters.")
	private String nationality;

	public CustomerProfileUpdateRequest() {
	}

	public CustomerProfileUpdateRequest(String firstName, String lastName, String mobileNumber,
			String alternativePhone, String addressLine1, String addressLine2, String city, String district,
			String province, String postalCode, String nationality) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.mobileNumber = mobileNumber;
		this.alternativePhone = alternativePhone;
		this.addressLine1 = addressLine1;
		this.addressLine2 = addressLine2;
		this.city = city;
		this.district = district;
		this.province = province;
		this.postalCode = postalCode;
		this.nationality = nationality;
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

	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}
}
