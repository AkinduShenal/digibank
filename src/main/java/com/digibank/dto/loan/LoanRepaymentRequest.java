package com.digibank.dto.loan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoanRepaymentRequest {
	@NotBlank(message = "Select the account to debit.")
	private String accountNumber;

	@NotBlank(message = "Enter your transaction PIN.")
	@Pattern(regexp = "^\\d{4}$", message = "Transaction PIN must contain four digits.")
	private String transactionPin;

	public String getAccountNumber() { return accountNumber; }
	public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
	public String getTransactionPin() { return transactionPin; }
	public void setTransactionPin(String transactionPin) { this.transactionPin = transactionPin; }
}
