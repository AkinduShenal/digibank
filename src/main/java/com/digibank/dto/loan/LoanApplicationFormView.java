package com.digibank.dto.loan;

import com.digibank.enums.LoanType;

import java.util.List;

public record LoanApplicationFormView(List<LoanAccountOption> accounts, LoanType[] loanTypes) {
}
