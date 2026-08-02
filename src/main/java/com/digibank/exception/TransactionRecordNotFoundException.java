package com.digibank.exception;

public class TransactionRecordNotFoundException extends RuntimeException {

	public TransactionRecordNotFoundException() {
		super("Transaction record was not found.");
	}
}
