package com.digibank.dto.bill;

import com.digibank.enums.BillPaymentStatus;
import com.digibank.enums.BillerCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillPaymentListView(String referenceNumber, String customerName, String providerName,
		BillerCategory category, String maskedConsumerReference, BigDecimal amount, BillPaymentStatus status,
		LocalDateTime paidAt) {
}
