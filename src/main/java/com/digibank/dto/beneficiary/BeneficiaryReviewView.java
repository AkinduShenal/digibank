package com.digibank.dto.beneficiary;

import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryType;
import com.digibank.enums.BeneficiaryVerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BeneficiaryReviewView {

	private final Long id;
	private final String customerNumber;
	private final String customerFullName;
	private final String beneficiaryName;
	private final String bankName;
	private final String bankCode;
	private final String branchName;
	private final String branchCode;
	private final String maskedAccountNumber;
	private final BeneficiaryAccountType accountType;
	private final BeneficiaryType beneficiaryType;
	private final BigDecimal transferLimit;
	private final BeneficiaryVerificationStatus verificationStatus;
	private final String verificationNote;
	private final LocalDateTime createdAt;

	public BeneficiaryReviewView(Long id, String customerNumber, String customerFullName, String beneficiaryName,
			String bankName, String bankCode, String branchName, String branchCode, String maskedAccountNumber,
			BeneficiaryAccountType accountType, BeneficiaryType beneficiaryType, BigDecimal transferLimit,
			BeneficiaryVerificationStatus verificationStatus, String verificationNote, LocalDateTime createdAt) {
		this.id = id;
		this.customerNumber = customerNumber;
		this.customerFullName = customerFullName;
		this.beneficiaryName = beneficiaryName;
		this.bankName = bankName;
		this.bankCode = bankCode;
		this.branchName = branchName;
		this.branchCode = branchCode;
		this.maskedAccountNumber = maskedAccountNumber;
		this.accountType = accountType;
		this.beneficiaryType = beneficiaryType;
		this.transferLimit = transferLimit;
		this.verificationStatus = verificationStatus;
		this.verificationNote = verificationNote;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getCustomerNumber() {
		return customerNumber;
	}

	public String getCustomerFullName() {
		return customerFullName;
	}

	public String getBeneficiaryName() {
		return beneficiaryName;
	}

	public String getBankName() {
		return bankName;
	}

	public String getBankCode() {
		return bankCode;
	}

	public String getBranchName() {
		return branchName;
	}

	public String getBranchCode() {
		return branchCode;
	}

	public String getMaskedAccountNumber() {
		return maskedAccountNumber;
	}

	public BeneficiaryAccountType getAccountType() {
		return accountType;
	}

	public BeneficiaryType getBeneficiaryType() {
		return beneficiaryType;
	}

	public BigDecimal getTransferLimit() {
		return transferLimit;
	}

	public BeneficiaryVerificationStatus getVerificationStatus() {
		return verificationStatus;
	}

	public String getVerificationNote() {
		return verificationNote;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
