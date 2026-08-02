package com.digibank.dto.loan;

import com.digibank.enums.LoanStatus;
import com.digibank.enums.LoanType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LoanApplicationView(String applicationNumber, String customerNumber, String customerName,
		String accountNumber, LoanType loanType, LoanStatus status, BigDecimal requestedAmount,
		BigDecimal approvedAmount, BigDecimal annualInterestRate, int termMonths, BigDecimal monthlyInstallment,
		BigDecimal monthlyIncome, String employmentStatus, String purpose, String reviewedBy,
		LocalDateTime reviewedAt, String reviewNote, LocalDateTime disbursedAt, LocalDateTime appliedAt,
		List<LoanScheduleView> schedule) {
}
