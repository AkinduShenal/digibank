package com.digibank.dto.beneficiary;

import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;
import com.digibank.enums.BeneficiaryVerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BeneficiaryDetailsView {

	private final Long id;
	private final String beneficiaryName;
	private final String nickname;
	private final String bankName;
	private final String bankCode;
	private final String branchName;
	private final String branchCode;
	private final String maskedAccountNumber;
	private final BeneficiaryAccountType accountType;
	private final BeneficiaryType beneficiaryType;
	private final BeneficiaryStatus status;
	private final boolean favourite;
	private final BigDecimal transferLimit;
	private final BeneficiaryVerificationStatus verificationStatus;
	private final String reviewedBy;
	private final LocalDateTime reviewedAt;
	private final String verificationNote;
	private final Long version;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;

	public BeneficiaryDetailsView(Long id, String beneficiaryName, String nickname, String bankName,
			String bankCode, String branchName, String branchCode, String maskedAccountNumber,
			BeneficiaryAccountType accountType, BeneficiaryType beneficiaryType, BeneficiaryStatus status,
			boolean favourite, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
		this(id, beneficiaryName, nickname, bankName, bankCode, branchName, branchCode, maskedAccountNumber,
				accountType, beneficiaryType, status, favourite, null, null, null, null, null, version, createdAt,
				updatedAt);
	}

	public BeneficiaryDetailsView(Long id, String beneficiaryName, String nickname, String bankName,
			String bankCode, String branchName, String branchCode, String maskedAccountNumber,
			BeneficiaryAccountType accountType, BeneficiaryType beneficiaryType, BeneficiaryStatus status,
			boolean favourite, BigDecimal transferLimit, BeneficiaryVerificationStatus verificationStatus,
			String reviewedBy, LocalDateTime reviewedAt, String verificationNote, Long version,
			LocalDateTime createdAt, LocalDateTime updatedAt) {
		this.id = id;
		this.beneficiaryName = beneficiaryName;
		this.nickname = nickname;
		this.bankName = bankName;
		this.bankCode = bankCode;
		this.branchName = branchName;
		this.branchCode = branchCode;
		this.maskedAccountNumber = maskedAccountNumber;
		this.accountType = accountType;
		this.beneficiaryType = beneficiaryType;
		this.status = status;
		this.favourite = favourite;
		this.transferLimit = transferLimit;
		this.verificationStatus = verificationStatus;
		this.reviewedBy = reviewedBy;
		this.reviewedAt = reviewedAt;
		this.verificationNote = verificationNote;
		this.version = version;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public String getBeneficiaryName() {
		return beneficiaryName;
	}

	public String getNickname() {
		return nickname;
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

	public BeneficiaryStatus getStatus() {
		return status;
	}

	public boolean isFavourite() {
		return favourite;
	}

	public BigDecimal getTransferLimit() {
		return transferLimit;
	}

	public BeneficiaryVerificationStatus getVerificationStatus() {
		return verificationStatus;
	}

	public String getReviewedBy() {
		return reviewedBy;
	}

	public LocalDateTime getReviewedAt() {
		return reviewedAt;
	}

	public String getVerificationNote() {
		return verificationNote;
	}

	public Long getVersion() {
		return version;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
