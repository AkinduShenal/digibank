package com.digibank.validation.validator;

import com.digibank.dto.auth.CustomerRegistrationRequest;
import com.digibank.enums.AccountType;
import com.digibank.validation.annotation.ValidInitialDeposit;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class InitialDepositValidator implements ConstraintValidator<ValidInitialDeposit, CustomerRegistrationRequest> {

	private static final BigDecimal SAVINGS_MINIMUM = new BigDecimal("1000.00");
	private static final BigDecimal CURRENT_MINIMUM = new BigDecimal("5000.00");

	@Override
	public boolean isValid(CustomerRegistrationRequest request, ConstraintValidatorContext context) {
		if (request == null || request.getAccountType() == null || request.getInitialDeposit() == null) {
			return true;
		}
		BigDecimal minimumDeposit = minimumDepositFor(request.getAccountType());
		if (minimumDeposit == null || request.getInitialDeposit().compareTo(minimumDeposit) >= 0) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(messageFor(request.getAccountType()))
				.addPropertyNode("initialDeposit")
				.addConstraintViolation();
		return false;
	}

	private BigDecimal minimumDepositFor(AccountType accountType) {
		if (accountType == AccountType.SAVINGS) {
			return SAVINGS_MINIMUM;
		}
		if (accountType == AccountType.CURRENT) {
			return CURRENT_MINIMUM;
		}
		return null;
	}

	private String messageFor(AccountType accountType) {
		if (accountType == AccountType.SAVINGS) {
			return "An initial deposit of at least LKR 1,000 is required for a savings account.";
		}
		if (accountType == AccountType.CURRENT) {
			return "An initial deposit of at least LKR 5,000 is required for a current account.";
		}
		return "Initial deposit does not meet the selected account type minimum.";
	}
}
