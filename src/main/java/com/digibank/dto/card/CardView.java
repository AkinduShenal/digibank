package com.digibank.dto.card;

import com.digibank.enums.CardStatus;
import com.digibank.enums.CardType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CardView(
		String requestNumber,
		String customerName,
		String customerNumber,
		String accountNumber,
		CardType cardType,
		CardStatus status,
		String cardholderName,
		String maskedCardNumber,
		LocalDate expiryDate,
		LocalDateTime requestedAt,
		String reviewedBy,
		LocalDateTime reviewedAt,
		String reviewNote,
		LocalDateTime activatedAt,
		LocalDateTime blockedAt,
		String blockReason,
		BigDecimal spendingLimit,
		LocalDateTime lostStolenAt,
		LocalDateTime cancelledAt,
		String cancellationReason) {
}
