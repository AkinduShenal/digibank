package com.digibank.util;

import com.digibank.exception.InvalidBeneficiaryException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountNumberNormalizerTest {

	private final AccountNumberNormalizer normalizer = new AccountNumberNormalizer();

	@Test
	void removesSpacesHyphensAndTrimsInput() {
		assertEquals("1234567890", normalizer.normalize(" 123 456-7890 "));
	}

	@Test
	void preservesValidDigits() {
		assertEquals("123456", normalizer.normalize("123456"));
	}

	@Test
	void rejectsNull() {
		assertThrows(InvalidBeneficiaryException.class, () -> normalizer.normalize(null));
	}

	@Test
	void rejectsBlank() {
		assertThrows(InvalidBeneficiaryException.class, () -> normalizer.normalize("   "));
	}

	@Test
	void rejectsLettersForDigitsOnlyPolicy() {
		assertThrows(InvalidBeneficiaryException.class, () -> normalizer.normalize("123ABC456"));
	}

	@Test
	void rejectsTooShortValue() {
		assertThrows(InvalidBeneficiaryException.class, () -> normalizer.normalize("12345"));
	}

	@Test
	void rejectsTooLongValue() {
		assertThrows(InvalidBeneficiaryException.class,
				() -> normalizer.normalize("12345678901234567890123456789012345"));
	}

	@Test
	void equivalentInputsProduceSameCanonicalValue() {
		assertEquals(normalizer.normalize("123-456 789"), normalizer.normalize("123456789"));
	}

	@Test
	void isValidReturnsFalseForMalformedInput() {
		assertFalse(normalizer.isValid("ABC123"));
		assertTrue(normalizer.isValid("123 456"));
	}
}
