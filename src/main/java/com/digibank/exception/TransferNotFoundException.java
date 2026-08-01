package com.digibank.exception;

public class TransferNotFoundException extends RuntimeException {

	public TransferNotFoundException() {
		super("The requested transfer record was not found.");
	}
}
