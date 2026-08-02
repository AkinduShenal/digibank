package com.digibank.dto.bill;

import com.digibank.enums.BillerProvider;
import com.digibank.enums.BillerSelectionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class BillPaymentRequest {
	@NotBlank(message = "Select a source account.")
	private String sourceAccountNumber;
	@NotNull(message = "Select a biller option.")
	private BillerSelectionType selectionType;
	private Long savedBillerId;
	private BillerProvider provider;
	private String consumerReference;
	private boolean saveBiller;
	private String nickname;
	@NotNull(message = "Enter the bill amount.")
	@DecimalMin(value = "10.00", message = "Minimum bill payment is LKR 10.00.")
	@DecimalMax(value = "1000000.00", message = "Maximum bill payment is LKR 1,000,000.00.")
	@Digits(integer = 7, fraction = 2, message = "Enter a valid amount with at most two decimals.")
	private BigDecimal amount;
	@NotBlank(message = "Enter your transaction PIN.")
	@Pattern(regexp = "\\d{4}", message = "Transaction PIN must contain exactly 4 digits.")
	private String transactionPin;
	@Size(max = 140, message = "Description cannot exceed 140 characters.")
	private String description;

	public String getSourceAccountNumber() { return sourceAccountNumber; }
	public void setSourceAccountNumber(String value) { sourceAccountNumber = value; }
	public BillerSelectionType getSelectionType() { return selectionType; }
	public void setSelectionType(BillerSelectionType value) { selectionType = value; }
	public Long getSavedBillerId() { return savedBillerId; }
	public void setSavedBillerId(Long value) { savedBillerId = value; }
	public BillerProvider getProvider() { return provider; }
	public void setProvider(BillerProvider value) { provider = value; }
	public String getConsumerReference() { return consumerReference; }
	public void setConsumerReference(String value) { consumerReference = value; }
	public boolean isSaveBiller() { return saveBiller; }
	public void setSaveBiller(boolean value) { saveBiller = value; }
	public String getNickname() { return nickname; }
	public void setNickname(String value) { nickname = value; }
	public BigDecimal getAmount() { return amount; }
	public void setAmount(BigDecimal value) { amount = value; }
	public String getTransactionPin() { return transactionPin; }
	public void setTransactionPin(String value) { transactionPin = value; }
	public String getDescription() { return description; }
	public void setDescription(String value) { description = value; }
}
