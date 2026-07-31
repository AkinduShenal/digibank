package com.digibank.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptTransactionPinEncoder implements TransactionPinEncoder {

	private final BCryptPasswordEncoder encoder;

	public BCryptTransactionPinEncoder() {
		this.encoder = new BCryptPasswordEncoder();
	}

	@Override
	public String encode(String rawTransactionPin) {
		return encoder.encode(rawTransactionPin);
	}
}
