package com.digibank.util;

import java.util.Set;

public final class BeneficiaryConstants {

	public static final String DIGIBANK_BANK_NAME = "DigiBank";
	public static final String DIGIBANK_BANK_CODE = "DIGIBANK";
	public static final int MIN_ACCOUNT_NUMBER_LENGTH = 6;
	public static final int MAX_ACCOUNT_NUMBER_LENGTH = 34;
	public static final int DEFAULT_PAGE_SIZE = 10;
	public static final int MAX_PAGE_SIZE = 50;
	public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
			"beneficiaryName",
			"bankName",
			"beneficiaryType",
			"createdAt",
			"updatedAt",
			"status",
			"favourite"
	);

	private BeneficiaryConstants() {
	}
}
