package com.digibank.dto.transfer;

import com.digibank.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StaffTransferView(String referenceNumber,String customerNumber,String customerName,
		String sourceAccount,String recipientName,String destinationBank,String destinationAccount,
		BigDecimal amount,TransferType transferType,TransferStatus status,String description,LocalDateTime completedAt,
		String reversalReference,String reversalReason,String reversedBy,LocalDateTime reversedAt) { }
