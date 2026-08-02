package com.digibank.dto.schedule;

import com.digibank.enums.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ScheduledPaymentRequest {
	@NotNull private ScheduledPaymentType paymentType;
	@NotBlank private String sourceAccountNumber;
	@NotNull @DecimalMin("0.01") @DecimalMax("1000000.00") @Digits(integer = 7, fraction = 2)
	private BigDecimal amount;
	@NotNull private ScheduleRecurrence recurrence = ScheduleRecurrence.ONCE;
	@NotNull @Future @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime nextExecutionAt;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate endDate;
	@Size(max = 140) private String description;
	@NotBlank @Pattern(regexp = "\\d{4}") private String transactionPin;
	private TransferRecipientType transferRecipientType;
	private Long beneficiaryId;
	private String destinationAccountNumber;
	private String ownDestinationAccountNumber;
	private BillerSelectionType billerSelectionType;
	private Long savedBillerId;
	private BillerProvider billerProvider;
	private String consumerReference;

	public ScheduledPaymentType getPaymentType(){return paymentType;} public void setPaymentType(ScheduledPaymentType v){paymentType=v;}
	public String getSourceAccountNumber(){return sourceAccountNumber;} public void setSourceAccountNumber(String v){sourceAccountNumber=v;}
	public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
	public ScheduleRecurrence getRecurrence(){return recurrence;} public void setRecurrence(ScheduleRecurrence v){recurrence=v;}
	public LocalDateTime getNextExecutionAt(){return nextExecutionAt;} public void setNextExecutionAt(LocalDateTime v){nextExecutionAt=v;}
	public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
	public String getDescription(){return description;} public void setDescription(String v){description=v;}
	public String getTransactionPin(){return transactionPin;} public void setTransactionPin(String v){transactionPin=v;}
	public TransferRecipientType getTransferRecipientType(){return transferRecipientType;} public void setTransferRecipientType(TransferRecipientType v){transferRecipientType=v;}
	public Long getBeneficiaryId(){return beneficiaryId;} public void setBeneficiaryId(Long v){beneficiaryId=v;}
	public String getDestinationAccountNumber(){return destinationAccountNumber;} public void setDestinationAccountNumber(String v){destinationAccountNumber=v;}
	public String getOwnDestinationAccountNumber(){return ownDestinationAccountNumber;} public void setOwnDestinationAccountNumber(String v){ownDestinationAccountNumber=v;}
	public BillerSelectionType getBillerSelectionType(){return billerSelectionType;} public void setBillerSelectionType(BillerSelectionType v){billerSelectionType=v;}
	public Long getSavedBillerId(){return savedBillerId;} public void setSavedBillerId(Long v){savedBillerId=v;}
	public BillerProvider getBillerProvider(){return billerProvider;} public void setBillerProvider(BillerProvider v){billerProvider=v;}
	public String getConsumerReference(){return consumerReference;} public void setConsumerReference(String v){consumerReference=v;}
}
