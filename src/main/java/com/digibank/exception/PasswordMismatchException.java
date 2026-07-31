package com.digibank.exception;

public class PasswordMismatchException extends RegistrationException {

	public PasswordMismatchException() {
		super("Passwords do not match.");
	}
}
