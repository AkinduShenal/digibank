package com.digibank.dto.card;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public class CardLimitRequest {
	@NotNull(message = "Enter a spending limit.")
	@DecimalMin(value = "1000.00", message = "Minimum card limit is LKR 1,000.00.")
	@DecimalMax(value = "1000000.00", message = "Maximum card limit is LKR 1,000,000.00.")
	@Digits(integer = 7, fraction = 2)
	private BigDecimal spendingLimit;
	@Pattern(regexp = "\\d{4}", message = "Transaction PIN must contain exactly 4 digits.")
	private String transactionPin;
	public BigDecimal getSpendingLimit() { return spendingLimit; }
	public void setSpendingLimit(BigDecimal value) { spendingLimit = value; }
	public String getTransactionPin() { return transactionPin; }
	public void setTransactionPin(String value) { transactionPin = value; }
}
