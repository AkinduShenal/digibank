package com.digibank.dto.loan;

import java.math.BigDecimal;

public record LoanAccountOption(String accountNumber, String accountType, BigDecimal balance) {
}
