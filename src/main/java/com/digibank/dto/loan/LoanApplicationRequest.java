package com.digibank.dto.loan;

import com.digibank.enums.LoanType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class LoanApplicationRequest {
	@NotBlank(message = "Select a disbursement account.")
	private String accountNumber;

	@NotNull(message = "Select a loan type.")
	private LoanType loanType;

	@NotNull(message = "Enter the requested amount.")
	@DecimalMin(value = "50000.00", message = "Minimum loan amount is LKR 50,000.")
	@DecimalMax(value = "5000000.00", message = "Maximum loan amount is LKR 5,000,000.")
	private BigDecimal requestedAmount;

	@Min(value = 6, message = "Minimum term is 6 months.")
	@Max(value = 60, message = "Maximum term is 60 months.")
	private int termMonths = 12;

	@NotNull(message = "Enter your monthly income.")
	@DecimalMin(value = "1.00", message = "Monthly income must be greater than zero.")
	private BigDecimal monthlyIncome;

	@NotBlank(message = "Enter your employment status.")
	@Size(max = 80, message = "Employment status is too long.")
	private String employmentStatus;

	@NotBlank(message = "Enter the purpose of this loan.")
	@Size(min = 10, max = 500, message = "Purpose must contain 10 to 500 characters.")
	private String purpose;

	public String getAccountNumber() { return accountNumber; }
	public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
	public LoanType getLoanType() { return loanType; }
	public void setLoanType(LoanType loanType) { this.loanType = loanType; }
	public BigDecimal getRequestedAmount() { return requestedAmount; }
	public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
	public int getTermMonths() { return termMonths; }
	public void setTermMonths(int termMonths) { this.termMonths = termMonths; }
	public BigDecimal getMonthlyIncome() { return monthlyIncome; }
	public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
	public String getEmploymentStatus() { return employmentStatus; }
	public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
	public String getPurpose() { return purpose; }
	public void setPurpose(String purpose) { this.purpose = purpose; }
}
