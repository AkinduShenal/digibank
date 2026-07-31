package com.digibank.util;

import com.digibank.exception.InvalidBeneficiaryException;
import org.springframework.stereotype.Component;

@Component
public class AccountNumberNormalizer {

	public String normalize(String rawAccountNumber) {
		if (rawAccountNumber == null) {
			throw new InvalidBeneficiaryException("Account number is required.");
		}
		String normalized = rawAccountNumber.trim().replaceAll("[\\s-]", "");
		if (normalized.isBlank()) {
			throw new InvalidBeneficiaryException("Account number is required.");
		}
		if (!normalized.matches("^\\d+$")) {
			throw new InvalidBeneficiaryException("Account number can contain digits, spaces and hyphens only.");
		}
		if (normalized.length() < BeneficiaryConstants.MIN_ACCOUNT_NUMBER_LENGTH) {
			throw new InvalidBeneficiaryException("Account number is too short.");
		}
		if (normalized.length() > BeneficiaryConstants.MAX_ACCOUNT_NUMBER_LENGTH) {
			throw new InvalidBeneficiaryException("Account number is too long.");
		}
		return normalized;
	}

	public boolean isValid(String rawAccountNumber) {
		try {
			normalize(rawAccountNumber);
			return true;
		}
		catch (InvalidBeneficiaryException ex) {
			return false;
		}
	}
}
