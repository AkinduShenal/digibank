package com.digibank.exception;

public class PinMismatchException extends RegistrationException {

	public PinMismatchException() {
		super("Transaction PINs do not match.");
	}
}
