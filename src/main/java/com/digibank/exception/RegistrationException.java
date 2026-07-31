package com.digibank.exception;

public abstract class RegistrationException extends RuntimeException {

	protected RegistrationException(String message) {
		super(message);
	}
}
