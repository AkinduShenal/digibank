package com.digibank.util;

import com.digibank.exception.InvalidBeneficiaryException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankCodeNormalizerTest {

	private final BankCodeNormalizer normalizer = new BankCodeNormalizer();

	@Test
	void trimsAndUppercasesUsingRootLocale() {
		assertEquals("ABC123", normalizer.normalize(" abc123 "));
	}

	@Test
	void normalizesAcceptedSpacingAndKeepsSeparators() {
		assertEquals("AB-C_01", normalizer.normalize(" ab - c_01 "));
	}

	@Test
	void rejectsBlank() {
		assertThrows(InvalidBeneficiaryException.class, () -> normalizer.normalize("   "));
	}

	@Test
	void rejectsIllegalCharacters() {
		assertThrows(InvalidBeneficiaryException.class, () -> normalizer.normalize("ABC@123"));
	}

	@Test
	void deterministicForEquivalentInput() {
		assertEquals(normalizer.normalize("abc-01"), normalizer.normalize(" ABC-01 "));
	}

	@Test
	void isValidReturnsFalseForInvalidInput() {
		assertFalse(normalizer.isValid("bank code!"));
		assertTrue(normalizer.isValid("bank_code-1"));
	}
}
