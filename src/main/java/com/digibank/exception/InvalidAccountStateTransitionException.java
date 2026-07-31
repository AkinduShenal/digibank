package com.digibank.exception;

public class InvalidAccountStateTransitionException extends RuntimeException {

	public InvalidAccountStateTransitionException(String message) {
		super(message);
	}
}
