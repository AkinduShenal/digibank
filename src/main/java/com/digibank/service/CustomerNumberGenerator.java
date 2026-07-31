package com.digibank.service;

import com.digibank.exception.NumberGenerationException;
import com.digibank.repository.CustomerRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Year;

@Component
public class CustomerNumberGenerator {

	private static final int MAX_ATTEMPTS = 20;
	private static final int RANDOM_BOUND = 1_000_000;

	private final CustomerRepository customerRepository;
	private final SecureRandom secureRandom;

	public CustomerNumberGenerator(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
		this.secureRandom = new SecureRandom();
	}

	public String generateUniqueCustomerNumber() {
		String year = String.valueOf(Year.now().getValue());
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			String candidate = "CUS" + year + String.format("%06d", secureRandom.nextInt(RANDOM_BOUND));
			if (customerRepository.findByCustomerNumber(candidate).isEmpty()) {
				return candidate;
			}
		}
		throw new NumberGenerationException("Unable to generate a unique customer number.");
	}
}
