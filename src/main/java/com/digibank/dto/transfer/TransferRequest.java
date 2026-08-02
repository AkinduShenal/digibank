package com.digibank.dto.transfer;

import com.digibank.enums.TransferRecipientType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class TransferRequest {

	@NotBlank(message = "A source account is required.")
	private String sourceAccountNumber;

	@NotNull(message = "Select how you want to choose the recipient.")
	private TransferRecipientType recipientType = TransferRecipientType.SAVED_BENEFICIARY;

	private Long beneficiaryId;

	@Size(max = 34, message = "DigiBank account number is too long.")
	private String destinationAccountNumber;

	private String ownDestinationAccountNumber;

	@NotNull(message = "Transfer amount is required.")
	@DecimalMin(value = "0.01", message = "Transfer amount must be at least LKR 0.01.")
	@Digits(integer = 17, fraction = 2, message = "Transfer amount can have at most two decimal places.")
	private BigDecimal amount;

	@Size(max = 140, message = "Description cannot exceed 140 characters.")
	private String description;

	@NotBlank(message = "Transaction PIN is required.")
	@Pattern(regexp = "^\\d{4}$", message = "Transaction PIN must be exactly 4 digits.")
	private String transactionPin;

	public String getSourceAccountNumber() { return sourceAccountNumber; }
	public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }
	public TransferRecipientType getRecipientType() { return recipientType; }
	public void setRecipientType(TransferRecipientType recipientType) { this.recipientType = recipientType; }
	public Long getBeneficiaryId() { return beneficiaryId; }
	public void setBeneficiaryId(Long beneficiaryId) { this.beneficiaryId = beneficiaryId; }
	public String getDestinationAccountNumber() { return destinationAccountNumber; }
	public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }
	public String getOwnDestinationAccountNumber() { return ownDestinationAccountNumber; }
	public void setOwnDestinationAccountNumber(String value) { ownDestinationAccountNumber = value; }
	public BigDecimal getAmount() { return amount; }
	public void setAmount(BigDecimal amount) { this.amount = amount; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getTransactionPin() { return transactionPin; }
	public void setTransactionPin(String transactionPin) { this.transactionPin = transactionPin; }
}
