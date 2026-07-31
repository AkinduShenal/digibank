package com.digibank.exception;

public class DuplicateIdentityException extends RegistrationException {

	public DuplicateIdentityException() {
		super("Identity number is already registered.");
	}
}
