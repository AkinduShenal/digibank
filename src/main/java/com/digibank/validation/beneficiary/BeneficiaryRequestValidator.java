package com.digibank.validation.beneficiary;

import com.digibank.dto.beneficiary.BeneficiaryCreateRequest;
import com.digibank.dto.beneficiary.BeneficiaryUpdateRequest;
import com.digibank.enums.BeneficiaryType;
import com.digibank.util.AccountNumberNormalizer;
import com.digibank.util.BankCodeNormalizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BeneficiaryRequestValidator implements ConstraintValidator<ValidBeneficiaryRequest, Object> {

	private final AccountNumberNormalizer accountNumberNormalizer = new AccountNumberNormalizer();
	private final BankCodeNormalizer bankCodeNormalizer = new BankCodeNormalizer();

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		if (value instanceof BeneficiaryCreateRequest request) {
			return validateCreate(request, context);
		}
		if (value instanceof BeneficiaryUpdateRequest request) {
			return validateUpdate(request, context);
		}
		return true;
	}

	private boolean validateCreate(BeneficiaryCreateRequest request, ConstraintValidatorContext context) {
		boolean valid = true;
		if (request.getBeneficiaryType() == null) {
			return true;
		}
		if (!accountNumberNormalizer.isValid(request.getAccountNumber())) {
			addViolation(context, "accountNumber", "Enter a valid account number using 6 to 34 digits.");
			valid = false;
		}
		if (request.getBeneficiaryType() == BeneficiaryType.EXTERNAL) {
			valid = requireText(context, request.getBankName(), "bankName", "Bank name is required for an external beneficiary.")
					&& valid;
			valid = requireBankCode(context, request.getBankCode()) && valid;
		}
		else if (request.getBankCode() != null && !request.getBankCode().isBlank()) {
			valid = validateBankCode(context, request.getBankCode(), "bankCode") && valid;
		}
		valid = validateOptionalBankCode(context, request.getBranchCode(), "branchCode",
				"Branch code can contain letters, digits, hyphens and underscores only.") && valid;
		return valid;
	}

	private boolean validateUpdate(BeneficiaryUpdateRequest request, ConstraintValidatorContext context) {
		boolean valid = requireText(context, request.getBankName(), "bankName", "Bank name is required.") ;
		valid = requireBankCode(context, request.getBankCode()) && valid;
		valid = validateOptionalBankCode(context, request.getBranchCode(), "branchCode",
				"Branch code can contain letters, digits, hyphens and underscores only.") && valid;
		return valid;
	}

	private boolean requireText(ConstraintValidatorContext context, String value, String field, String message) {
		if (value == null || value.trim().isEmpty()) {
			addViolation(context, field, message);
			return false;
		}
		return true;
	}

	private boolean requireBankCode(ConstraintValidatorContext context, String value) {
		if (value == null || value.trim().isEmpty()) {
			addViolation(context, "bankCode", "Bank code is required.");
			return false;
		}
		return validateBankCode(context, value, "bankCode");
	}

	private boolean validateBankCode(ConstraintValidatorContext context, String value, String field) {
		if (!bankCodeNormalizer.isValid(value)) {
			addViolation(context, field, "Bank code can contain letters, digits, hyphens and underscores only.");
			return false;
		}
		return true;
	}

	private boolean validateOptionalBankCode(ConstraintValidatorContext context, String value, String field,
			String message) {
		if (value == null || value.isBlank()) {
			return true;
		}
		if (!bankCodeNormalizer.isValid(value)) {
			addViolation(context, field, message);
			return false;
		}
		return true;
	}

	private void addViolation(ConstraintValidatorContext context, String field, String message) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message)
				.addPropertyNode(field)
				.addConstraintViolation();
	}
}
