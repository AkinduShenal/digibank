package com.digibank.dto.bill;

import java.math.BigDecimal;

public record BillAccountOption(String accountNumber, String maskedAccountNumber, String accountType,
		BigDecimal availableBalance) {
}
