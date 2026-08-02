package com.digibank.enums;

public enum CardType {
	DEBIT("Debit card"),
	CREDIT("Credit card");

	private final String displayName;

	CardType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
