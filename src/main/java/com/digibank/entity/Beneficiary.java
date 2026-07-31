package com.digibank.entity;

import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Column(name = "beneficiary_name", nullable = false, length = 120)
	private String beneficiaryName;

	@Column(name = "nickname", length = 80)
	private String nickname;

	@Column(name = "bank_name", nullable = false, length = 120)
	private String bankName;

	@Column(name = "bank_code", nullable = false, length = 20)
	private String bankCode;

	@Column(name = "branch_name", length = 120)
	private String branchName;

	@Column(name = "branch_code", length = 20)
	private String branchCode;

	@Column(name = "account_number", nullable = false, length = 34)
	private String accountNumber;

	@Column(name = "normalized_account_number", nullable = false, length = 34)
	private String normalizedAccountNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_type", nullable = false, length = 20)
	private BeneficiaryAccountType accountType;

	@Enumerated(EnumType.STRING)
	@Column(name = "beneficiary_type", nullable = false, length = 20)
	private BeneficiaryType beneficiaryType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private BeneficiaryStatus status;

	@Column(name = "is_favourite", nullable = false)
	private boolean favourite;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected Beneficiary() {
	}

	public Beneficiary(Customer customer, String beneficiaryName, String bankName, String bankCode,
			String accountNumber, String normalizedAccountNumber, BeneficiaryAccountType accountType,
			BeneficiaryType beneficiaryType) {
		this.customer = customer;
		this.beneficiaryName = beneficiaryName;
		this.bankName = bankName;
		this.bankCode = bankCode;
		this.accountNumber = accountNumber;
		this.normalizedAccountNumber = normalizedAccountNumber;
		this.accountType = accountType;
		this.beneficiaryType = beneficiaryType;
		this.status = BeneficiaryStatus.ACTIVE;
		this.favourite = false;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public String getBeneficiaryName() {
		return beneficiaryName;
	}

	public void setBeneficiaryName(String beneficiaryName) {
		this.beneficiaryName = beneficiaryName;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getBankCode() {
		return bankCode;
	}

	public void setBankCode(String bankCode) {
		this.bankCode = bankCode;
	}

	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	public String getBranchCode() {
		return branchCode;
	}

	public void setBranchCode(String branchCode) {
		this.branchCode = branchCode;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getNormalizedAccountNumber() {
		return normalizedAccountNumber;
	}

	public void setNormalizedAccountNumber(String normalizedAccountNumber) {
		this.normalizedAccountNumber = normalizedAccountNumber;
	}

	public BeneficiaryAccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(BeneficiaryAccountType accountType) {
		this.accountType = accountType;
	}

	public BeneficiaryType getBeneficiaryType() {
		return beneficiaryType;
	}

	public void setBeneficiaryType(BeneficiaryType beneficiaryType) {
		this.beneficiaryType = beneficiaryType;
	}

	public BeneficiaryStatus getStatus() {
		return status;
	}

	public void setStatus(BeneficiaryStatus status) {
		this.status = status;
	}

	public boolean isFavourite() {
		return favourite;
	}

	public void setFavourite(boolean favourite) {
		this.favourite = favourite;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
}
