package com.digibank.dto.transaction;

import com.digibank.enums.AccountType;

public record StatementAccountOption(String accountNumber, String maskedAccountNumber, AccountType accountType) {
}
