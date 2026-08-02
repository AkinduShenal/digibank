package com.digibank.entity;

import com.digibank.enums.RepaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_repayment_schedule")
public class LoanRepaymentSchedule extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "loan_application_id", nullable = false)
	private LoanApplication loanApplication;

	@Column(name = "installment_number", nullable = false)
	private int installmentNumber;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal principalAmount;

	@Column(name = "interest_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal interestAmount;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private RepaymentStatus status;

	@Column(name = "paid_at")
	private LocalDateTime paidAt;

	@Column(name = "payment_reference", length = 32)
	private String paymentReference;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "paid_from_account_id")
	private BankAccount paidFromAccount;

	@Column(name = "balance_after", precision = 19, scale = 2)
	private BigDecimal balanceAfter;

	protected LoanRepaymentSchedule() {
	}

	public LoanRepaymentSchedule(LoanApplication loanApplication, int installmentNumber, LocalDate dueDate,
			BigDecimal principalAmount, BigDecimal interestAmount, BigDecimal totalAmount) {
		this.loanApplication = loanApplication;
		this.installmentNumber = installmentNumber;
		this.dueDate = dueDate;
		this.principalAmount = principalAmount;
		this.interestAmount = interestAmount;
		this.totalAmount = totalAmount;
		this.status = RepaymentStatus.SCHEDULED;
	}

	public LoanApplication getLoanApplication() { return loanApplication; }
	public int getInstallmentNumber() { return installmentNumber; }
	public LocalDate getDueDate() { return dueDate; }
	public BigDecimal getPrincipalAmount() { return principalAmount; }
	public BigDecimal getInterestAmount() { return interestAmount; }
	public BigDecimal getTotalAmount() { return totalAmount; }
	public RepaymentStatus getStatus() { return status; }
	public LocalDateTime getPaidAt() { return paidAt; }
	public String getPaymentReference() { return paymentReference; }
	public BankAccount getPaidFromAccount() { return paidFromAccount; }
	public BigDecimal getBalanceAfter() { return balanceAfter; }

	public void markPaid(String reference, BankAccount account, BigDecimal accountBalance, LocalDateTime when) {
		this.status = RepaymentStatus.PAID;
		this.paymentReference = reference;
		this.paidFromAccount = account;
		this.balanceAfter = accountBalance;
		this.paidAt = when;
	}

	public void markOverdue() {
		if (status == RepaymentStatus.SCHEDULED) {
			status = RepaymentStatus.OVERDUE;
		}
	}
}
