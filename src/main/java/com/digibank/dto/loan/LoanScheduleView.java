package com.digibank.dto.loan;

import com.digibank.enums.RepaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoanScheduleView(int installmentNumber, LocalDate dueDate, BigDecimal principalAmount,
		BigDecimal interestAmount, BigDecimal totalAmount, RepaymentStatus status, boolean payable,
		String paymentReference, LocalDateTime paidAt) {
}
