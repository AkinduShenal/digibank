package com.digibank.util;

import com.digibank.exception.InvalidBeneficiaryException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class BankCodeNormalizer {

	public String normalize(String rawBankCode) {
		if (rawBankCode == null) {
			throw new InvalidBeneficiaryException("Bank code is required.");
		}
		String normalized = rawBankCode.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
		if (normalized.isBlank()) {
			throw new InvalidBeneficiaryException("Bank code is required.");
		}
		if (normalized.length() > 20) {
			throw new InvalidBeneficiaryException("Bank code must not exceed 20 characters.");
		}
		if (!normalized.matches("^[A-Z0-9_-]+$")) {
			throw new InvalidBeneficiaryException("Bank code can contain letters, digits, hyphens and underscores only.");
		}
		return normalized;
	}

	public boolean isValid(String rawBankCode) {
		try {
			normalize(rawBankCode);
			return true;
		}
		catch (InvalidBeneficiaryException ex) {
			return false;
		}
	}
}
