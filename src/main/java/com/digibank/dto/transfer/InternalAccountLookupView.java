package com.digibank.dto.transfer;

public record InternalAccountLookupView(
		boolean available,
		String accountHolderName,
		String maskedAccountNumber) {

	public static InternalAccountLookupView unavailable() {
		return new InternalAccountLookupView(false, null, null);
	}
}
