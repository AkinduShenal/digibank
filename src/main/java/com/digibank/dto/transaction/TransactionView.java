package com.digibank.dto.transaction;

import com.digibank.enums.AccountTransactionType;
import com.digibank.enums.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionView(
		Long id,
		String accountMasked,
		String referenceNumber,
		TransactionDirection direction,
		AccountTransactionType transactionType,
		BigDecimal amount,
		BigDecimal balanceAfter,
		String counterpartyName,
		String counterpartyAccountMasked,
		String description,
		LocalDateTime occurredAt) {
}
