package com.digibank.service;

import com.digibank.exception.NumberGenerationException;
import com.digibank.repository.BankAccountRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

	private static final int MAX_ATTEMPTS = 20;
	private static final long LOWEST_12_DIGIT_NUMBER = 100_000_000_000L;
	private static final long RANGE_12_DIGIT_NUMBER = 900_000_000_000L;

	private final BankAccountRepository bankAccountRepository;
	private final SecureRandom secureRandom;

	public AccountNumberGenerator(BankAccountRepository bankAccountRepository) {
		this.bankAccountRepository = bankAccountRepository;
		this.secureRandom = new SecureRandom();
	}

	public String generateUniqueAccountNumber() {
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			long number = LOWEST_12_DIGIT_NUMBER + secureRandom.nextLong(RANGE_12_DIGIT_NUMBER);
			String candidate = String.valueOf(number);
			if (!bankAccountRepository.existsByAccountNumber(candidate)) {
				return candidate;
			}
		}
		throw new NumberGenerationException("Unable to generate a unique account number.");
	}
}
