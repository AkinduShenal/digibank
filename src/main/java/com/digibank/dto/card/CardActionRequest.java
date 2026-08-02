package com.digibank.dto.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CardActionRequest {
	@NotBlank(message = "Enter your transaction PIN.")
	@Pattern(regexp = "\\d{4}", message = "Transaction PIN must contain exactly 4 digits.")
	private String transactionPin;

	public String getTransactionPin() { return transactionPin; }
	public void setTransactionPin(String transactionPin) { this.transactionPin = transactionPin; }
}
