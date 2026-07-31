package com.digibank.exception;

public class DuplicateEmailException extends RegistrationException {

	public DuplicateEmailException(String email) {
		super("Email is already registered: " + email);
	}
}
