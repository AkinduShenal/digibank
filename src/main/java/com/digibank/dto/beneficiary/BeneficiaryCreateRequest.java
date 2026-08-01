package com.digibank.dto.beneficiary;

import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryType;
import com.digibank.validation.beneficiary.ValidBeneficiaryRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import static com.digibank.util.BeneficiaryConstants.DEFAULT_TRANSFER_LIMIT;

@ValidBeneficiaryRequest
public class BeneficiaryCreateRequest {

	@NotBlank(message = "Beneficiary name is required.")
	@Size(max = 120, message = "Beneficiary name must not exceed 120 characters.")
	private String beneficiaryName;

	@Size(max = 80, message = "Nickname must not exceed 80 characters.")
	@Pattern(regexp = "^$|^(?!\\s*$).+", message = "Nickname cannot contain only spaces.")
	private String nickname;

	@Size(max = 120, message = "Bank name must not exceed 120 characters.")
	private String bankName;

	@Size(max = 20, message = "Bank code must not exceed 20 characters.")
	private String bankCode;

	@Size(max = 120, message = "Branch name must not exceed 120 characters.")
	private String branchName;

	@Size(max = 20, message = "Branch code must not exceed 20 characters.")
	private String branchCode;

	@NotBlank(message = "Account number is required.")
	private String accountNumber;

	@NotNull(message = "Account type is required.")
	private BeneficiaryAccountType accountType;

	@NotNull(message = "Beneficiary type is required.")
	private BeneficiaryType beneficiaryType;

	@NotNull(message = "Transfer limit is required.")
	@DecimalMin(value = "0.01", message = "Transfer limit must be at least LKR 0.01.")
	@DecimalMax(value = "1000000.00", message = "Transfer limit cannot exceed LKR 1,000,000.00.")
	@Digits(integer = 17, fraction = 2, message = "Transfer limit can have at most two decimal places.")
	private BigDecimal transferLimit = DEFAULT_TRANSFER_LIMIT;

	public BeneficiaryCreateRequest() {
	}

	public BeneficiaryCreateRequest(String beneficiaryName, String nickname, String bankName, String bankCode,
			String branchName, String branchCode, String accountNumber, BeneficiaryAccountType accountType,
			BeneficiaryType beneficiaryType) {
		this.beneficiaryName = beneficiaryName;
		this.nickname = nickname;
		this.bankName = bankName;
		this.bankCode = bankCode;
		this.branchName = branchName;
		this.branchCode = branchCode;
		this.accountNumber = accountNumber;
		this.accountType = accountType;
		this.beneficiaryType = beneficiaryType;
	}

	public BeneficiaryCreateRequest(String beneficiaryName, String nickname, String bankName, String bankCode,
			String branchName, String branchCode, String accountNumber, BeneficiaryAccountType accountType,
			BeneficiaryType beneficiaryType, BigDecimal transferLimit) {
		this(beneficiaryName, nickname, bankName, bankCode, branchName, branchCode, accountNumber, accountType,
				beneficiaryType);
		this.transferLimit = transferLimit;
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

	public BigDecimal getTransferLimit() {
		return transferLimit;
	}

	public void setTransferLimit(BigDecimal transferLimit) {
		this.transferLimit = transferLimit;
	}
}
