package com.digibank.util;

import org.springframework.stereotype.Component;

@Component
public class SensitiveDataMasker {

	public String maskAccountNumber(String accountNumber) {
		String trimmed = trim(accountNumber);
		if (trimmed == null) {
			return "****";
		}
		if (trimmed.length() < 4) {
			return "*".repeat(trimmed.length());
		}
		if (trimmed.length() == 4) {
			return "****";
		}
		return "*".repeat(trimmed.length() - 4) + trimmed.substring(trimmed.length() - 4);
	}

	private String trim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
