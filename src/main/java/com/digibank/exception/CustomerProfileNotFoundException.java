package com.digibank.exception;

public class CustomerProfileNotFoundException extends RuntimeException {

	public CustomerProfileNotFoundException() {
		super("Customer profile was not found for the authenticated user.");
	}
}
