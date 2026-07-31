package com.digibank.dto.beneficiary;

import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;

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
	private final Long version;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;

	public BeneficiaryDetailsView(Long id, String beneficiaryName, String nickname, String bankName,
			String bankCode, String branchName, String branchCode, String maskedAccountNumber,
			BeneficiaryAccountType accountType, BeneficiaryType beneficiaryType, BeneficiaryStatus status,
			boolean favourite, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
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
