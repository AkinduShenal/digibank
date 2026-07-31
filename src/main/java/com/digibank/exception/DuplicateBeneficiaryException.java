package com.digibank.exception;

public class DuplicateBeneficiaryException extends RuntimeException {

	public DuplicateBeneficiaryException() {
		super("This beneficiary already exists.");
	}
}
