package com.digibank.validation.validator;

import com.digibank.dto.auth.CustomerRegistrationRequest;
import com.digibank.validation.annotation.ValidTransactionPin;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class TransactionPinValidator implements ConstraintValidator<ValidTransactionPin, CustomerRegistrationRequest> {

	private static final Set<String> OBVIOUS_PINS = Set.of("0000", "1111", "1234", "4321");

	@Override
	public boolean isValid(CustomerRegistrationRequest request, ConstraintValidatorContext context) {
		if (request == null) {
			return true;
		}
		boolean valid = true;
		context.disableDefaultConstraintViolation();

		String transactionPin = request.getTransactionPin();
		String confirmTransactionPin = request.getConfirmTransactionPin();

		if (transactionPin != null && OBVIOUS_PINS.contains(transactionPin)) {
			addFieldMessage(context, "transactionPin", "Choose a less obvious transaction PIN.");
			valid = false;
		}
		if (transactionPin != null && confirmTransactionPin != null && !transactionPin.equals(confirmTransactionPin)) {
			addFieldMessage(context, "confirmTransactionPin", "Transaction PINs do not match.");
			valid = false;
		}
		return valid;
	}

	private void addFieldMessage(ConstraintValidatorContext context, String fieldName, String message) {
		context.buildConstraintViolationWithTemplate(message)
				.addPropertyNode(fieldName)
				.addConstraintViolation();
	}
}
