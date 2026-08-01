package com.digibank.dto.transfer;

import com.digibank.enums.AccountType;

import java.math.BigDecimal;

public record TransferAccountOption(String accountNumber, String maskedAccountNumber, AccountType accountType,
		BigDecimal availableBalance) {
}
