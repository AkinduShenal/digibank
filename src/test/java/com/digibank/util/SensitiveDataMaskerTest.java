package com.digibank.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SensitiveDataMaskerTest {

	private final SensitiveDataMasker masker = new SensitiveDataMasker();

	@Test
	void masksStandardAccountNumber() {
		assertEquals("******7890", masker.maskAccountNumber("1234567890"));
	}

	@Test
	void masksExactlyFourCharacters() {
		assertEquals("****", masker.maskAccountNumber("1234"));
	}

	@Test
	void masksFewerThanFourCharacters() {
		assertEquals("**", masker.maskAccountNumber("12"));
	}

	@Test
	void handlesNullSafely() {
		assertEquals("****", masker.maskAccountNumber(null));
	}

	@Test
	void handlesBlankSafely() {
		assertEquals("****", masker.maskAccountNumber("   "));
	}

	@Test
	void neverRevealsCompleteValue() {
		String value = "1234567890";
		assertNotEquals(value, masker.maskAccountNumber(value));
	}
}
