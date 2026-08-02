package com.digibank.entity;

import com.digibank.enums.LoanStatus;
import com.digibank.enums.LoanType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_applications", uniqueConstraints = {
		@UniqueConstraint(name = "uk_loan_applications_number", columnNames = "application_number")
})
public class LoanApplication extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "disbursement_account_id", nullable = false)
	private BankAccount disbursementAccount;

	@Column(name = "application_number", nullable = false, length = 32)
	private String applicationNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "loan_type", nullable = false, length = 30)
	private LoanType loanType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private LoanStatus status;

	@Column(name = "requested_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal requestedAmount;

	@Column(name = "approved_amount", precision = 19, scale = 2)
	private BigDecimal approvedAmount;

	@Column(name = "annual_interest_rate", nullable = false, precision = 7, scale = 4)
	private BigDecimal annualInterestRate;

	@Column(name = "term_months", nullable = false)
	private int termMonths;

	@Column(name = "monthly_installment", nullable = false, precision = 19, scale = 2)
	private BigDecimal monthlyInstallment;

	@Column(name = "monthly_income", nullable = false, precision = 19, scale = 2)
	private BigDecimal monthlyIncome;

	@Column(name = "employment_status", nullable = false, length = 80)
	private String employmentStatus;

	@Column(name = "purpose", nullable = false, length = 500)
	private String purpose;

	@Column(name = "reviewed_by", length = 30)
	private String reviewedBy;

	@Column(name = "reviewed_at")
	private LocalDateTime reviewedAt;

	@Column(name = "review_note", length = 500)
	private String reviewNote;

	@Column(name = "disbursed_at")
	private LocalDateTime disbursedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected LoanApplication() {
	}

	public LoanApplication(Customer customer, BankAccount account, String applicationNumber, LoanType loanType,
			BigDecimal requestedAmount, BigDecimal annualInterestRate, int termMonths,
			BigDecimal monthlyInstallment, BigDecimal monthlyIncome, String employmentStatus, String purpose) {
		this.customer = customer;
		this.disbursementAccount = account;
		this.applicationNumber = applicationNumber;
		this.loanType = loanType;
		this.status = LoanStatus.PENDING_REVIEW;
		this.requestedAmount = requestedAmount;
		this.annualInterestRate = annualInterestRate;
		this.termMonths = termMonths;
		this.monthlyInstallment = monthlyInstallment;
		this.monthlyIncome = monthlyIncome;
		this.employmentStatus = employmentStatus;
		this.purpose = purpose;
	}

	public void disburse(String reviewer, String note, BigDecimal amount, BigDecimal installment, LocalDateTime when) {
		this.status = LoanStatus.DISBURSED;
		this.reviewedBy = reviewer;
		this.reviewNote = note;
		this.reviewedAt = when;
		this.approvedAmount = amount;
		this.monthlyInstallment = installment;
		this.disbursedAt = when;
	}

	public void reject(String reviewer, String reason, LocalDateTime when) {
		this.status = LoanStatus.REJECTED;
		this.reviewedBy = reviewer;
		this.reviewNote = reason;
		this.reviewedAt = when;
	}

	public void close() {
		this.status = LoanStatus.CLOSED;
	}

	public Customer getCustomer() { return customer; }
	public BankAccount getDisbursementAccount() { return disbursementAccount; }
	public String getApplicationNumber() { return applicationNumber; }
	public LoanType getLoanType() { return loanType; }
	public LoanStatus getStatus() { return status; }
	public BigDecimal getRequestedAmount() { return requestedAmount; }
	public BigDecimal getApprovedAmount() { return approvedAmount; }
	public BigDecimal getAnnualInterestRate() { return annualInterestRate; }
	public int getTermMonths() { return termMonths; }
	public BigDecimal getMonthlyInstallment() { return monthlyInstallment; }
	public BigDecimal getMonthlyIncome() { return monthlyIncome; }
	public String getEmploymentStatus() { return employmentStatus; }
	public String getPurpose() { return purpose; }
	public String getReviewedBy() { return reviewedBy; }
	public LocalDateTime getReviewedAt() { return reviewedAt; }
	public String getReviewNote() { return reviewNote; }
	public LocalDateTime getDisbursedAt() { return disbursedAt; }
	public long getVersion() { return version; }
}
