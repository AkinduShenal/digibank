package com.digibank.exception;

public class UnderageCustomerException extends RegistrationException {

	public UnderageCustomerException() {
		super("You must be at least 18 years old.");
	}
}
