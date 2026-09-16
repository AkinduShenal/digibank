package com.digibank.dto.schedule;

import com.digibank.enums.ScheduleRecurrence;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ScheduledPaymentUpdateRequest {
	@NotNull @DecimalMin("0.01") @DecimalMax("1000000.00") @Digits(integer=7, fraction=2) private BigDecimal amount;
	@NotNull(message = "Choose a repeat setting.") private ScheduleRecurrence recurrence;
	@NotNull(message = "Choose the next payment date and time.") @Future(message = "Payment date and time must be in the future.") @DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm") private LocalDateTime nextExecutionAt;
	@DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate endDate;
	@Size(max=140) private String description;
	public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
	public ScheduleRecurrence getRecurrence(){return recurrence;} public void setRecurrence(ScheduleRecurrence v){recurrence=v;}
	public LocalDateTime getNextExecutionAt(){return nextExecutionAt;} public void setNextExecutionAt(LocalDateTime v){nextExecutionAt=v;}
	public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
	public String getDescription(){return description;} public void setDescription(String v){description=v;}
}
