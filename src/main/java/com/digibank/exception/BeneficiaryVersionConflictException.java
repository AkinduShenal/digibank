package com.digibank.exception;

public class BeneficiaryVersionConflictException extends RuntimeException {

	public BeneficiaryVersionConflictException() {
		super("Beneficiary details were changed by another request. Please reload and try again.");
	}
}
