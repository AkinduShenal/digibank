package com.digibank.util;

import java.math.BigDecimal;
import java.util.Set;

public final class BeneficiaryConstants {

	public static final String DIGIBANK_BANK_NAME = "DigiBank";
	public static final String DIGIBANK_BANK_CODE = "DIGIBANK";
	public static final int MIN_ACCOUNT_NUMBER_LENGTH = 6;
	public static final int MAX_ACCOUNT_NUMBER_LENGTH = 34;
	public static final int DEFAULT_PAGE_SIZE = 10;
	public static final int MAX_PAGE_SIZE = 50;
	public static final BigDecimal DEFAULT_TRANSFER_LIMIT = new BigDecimal("100000.00");
	public static final BigDecimal MIN_TRANSFER_LIMIT = new BigDecimal("0.01");
	public static final BigDecimal MAX_TRANSFER_LIMIT = new BigDecimal("1000000.00");
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
