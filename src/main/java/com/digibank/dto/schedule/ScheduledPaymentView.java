package com.digibank.dto.schedule;

import com.digibank.enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ScheduledPaymentView(String reference, ScheduledPaymentType paymentType, ScheduleStatus status,
		ScheduleRecurrence recurrence, String sourceAccount, String destination, BigDecimal amount,
		String description, LocalDateTime nextExecutionAt, LocalDate endDate, int executionCount,
		String lastExecutionReference, LocalDateTime lastExecutedAt, String failureReason) { }
