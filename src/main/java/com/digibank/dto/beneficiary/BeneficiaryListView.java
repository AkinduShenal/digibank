package com.digibank.dto.beneficiary;

import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;

import java.time.LocalDateTime;

public class BeneficiaryListView {

	private final Long id;
	private final String beneficiaryName;
	private final String nickname;
	private final String bankName;
	private final String maskedAccountNumber;
	private final BeneficiaryAccountType accountType;
	private final BeneficiaryType beneficiaryType;
	private final BeneficiaryStatus status;
	private final boolean favourite;
	private final LocalDateTime createdAt;

	public BeneficiaryListView(Long id, String beneficiaryName, String nickname, String bankName,
			String maskedAccountNumber, BeneficiaryAccountType accountType, BeneficiaryType beneficiaryType,
			BeneficiaryStatus status, boolean favourite, LocalDateTime createdAt) {
		this.id = id;
		this.beneficiaryName = beneficiaryName;
		this.nickname = nickname;
		this.bankName = bankName;
		this.maskedAccountNumber = maskedAccountNumber;
		this.accountType = accountType;
		this.beneficiaryType = beneficiaryType;
		this.status = status;
		this.favourite = favourite;
		this.createdAt = createdAt;
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
