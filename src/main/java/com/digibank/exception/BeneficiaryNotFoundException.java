package com.digibank.exception;

public class BeneficiaryNotFoundException extends RuntimeException {

	public BeneficiaryNotFoundException() {
		super("Beneficiary is not available.");
	}
}
