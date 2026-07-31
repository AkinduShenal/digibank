package com.digibank.exception;

public class AccountAccessDeniedException extends RuntimeException {

	public AccountAccessDeniedException() {
		super("This account is not available for the current customer.");
	}
}
