package com.digibank.dto.bill;

import com.digibank.enums.BillPaymentStatus;
import com.digibank.enums.BillerCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillPaymentDetailsView(String referenceNumber, String customerName, String customerNumber,
		String maskedSourceAccount, String providerName, BillerCategory category, String maskedConsumerReference,
		BigDecimal amount, BillPaymentStatus status, BigDecimal sourceBalanceAfter, String description,
		LocalDateTime paidAt) {
}
