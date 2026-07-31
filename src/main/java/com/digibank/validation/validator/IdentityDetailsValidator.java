package com.digibank.validation.validator;

import com.digibank.dto.auth.CustomerRegistrationRequest;
import com.digibank.enums.IdentityType;
import com.digibank.validation.annotation.ValidIdentityDetails;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IdentityDetailsValidator implements ConstraintValidator<ValidIdentityDetails, CustomerRegistrationRequest> {

	private static final String OLD_NIC_PATTERN = "^\\d{9}[VvXx]$";
	private static final String NEW_NIC_PATTERN = "^\\d{12}$";
	private static final String PASSPORT_PATTERN = "^[A-Za-z0-9]{6,15}$";

	@Override
	public boolean isValid(CustomerRegistrationRequest request, ConstraintValidatorContext context) {
		if (request == null || request.getIdentityType() == null || request.getIdentityNumber() == null) {
			return true;
		}
		String identityNumber = request.getIdentityNumber().trim();
		boolean valid = isValidIdentityNumber(request.getIdentityType(), identityNumber);
		if (!valid) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(messageFor(request.getIdentityType()))
					.addPropertyNode("identityNumber")
					.addConstraintViolation();
		}
		return valid;
	}

	private boolean isValidIdentityNumber(IdentityType identityType, String identityNumber) {
		if (identityType == IdentityType.NATIONAL_ID) {
			return identityNumber.matches(OLD_NIC_PATTERN) || identityNumber.matches(NEW_NIC_PATTERN);
		}
		if (identityType == IdentityType.PASSPORT) {
			return identityNumber.matches(PASSPORT_PATTERN);
		}
		return false;
	}

	private String messageFor(IdentityType identityType) {
		if (identityType == IdentityType.NATIONAL_ID) {
			return "Enter a valid Sri Lankan NIC number.";
		}
		if (identityType == IdentityType.PASSPORT) {
			return "Passport number must contain 6 to 15 letters or numbers.";
		}
		return "Identity number format does not match the selected identity type.";
	}
}
