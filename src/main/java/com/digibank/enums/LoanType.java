package com.digibank.enums;

import java.math.BigDecimal;

public enum LoanType {
	PERSONAL("Personal loan", new BigDecimal("12.00")),
	EDUCATION("Education loan", new BigDecimal("8.50")),
	HOME("Home loan", new BigDecimal("7.50")),
	VEHICLE("Vehicle loan", new BigDecimal("9.50")),
	BUSINESS("Business loan", new BigDecimal("11.00"));

	private final String displayName;
	private final BigDecimal annualInterestRate;

	LoanType(String displayName, BigDecimal annualInterestRate) {
		this.displayName = displayName;
		this.annualInterestRate = annualInterestRate;
	}

	public String getDisplayName() { return displayName; }
	public BigDecimal getAnnualInterestRate() { return annualInterestRate; }
}
