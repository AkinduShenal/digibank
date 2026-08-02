package com.digibank.dto.transfer;

import com.digibank.enums.CurrencyCode;
import com.digibank.enums.TransferStatus;
import com.digibank.enums.TransferType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferListView(String referenceNumber, String beneficiaryName, String destinationBank,
		String destinationAccountMasked, BigDecimal amount, CurrencyCode currencyCode, TransferType transferType,
		TransferStatus status, LocalDateTime createdAt) {
}
