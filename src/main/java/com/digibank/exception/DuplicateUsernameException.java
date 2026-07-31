package com.digibank.exception;

public class DuplicateUsernameException extends RegistrationException {

	public DuplicateUsernameException(String username) {
		super("Username is already registered: " + username);
	}
}
