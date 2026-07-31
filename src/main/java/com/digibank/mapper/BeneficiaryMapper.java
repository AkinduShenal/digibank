package com.digibank.mapper;

import com.digibank.dto.beneficiary.BeneficiaryDetailsView;
import com.digibank.dto.beneficiary.BeneficiaryListView;
import com.digibank.dto.beneficiary.BeneficiaryUpdateRequest;
import com.digibank.entity.Beneficiary;
import com.digibank.util.SensitiveDataMasker;
import org.springframework.stereotype.Component;

@Component
public class BeneficiaryMapper {

	private final SensitiveDataMasker sensitiveDataMasker;

	public BeneficiaryMapper(SensitiveDataMasker sensitiveDataMasker) {
		this.sensitiveDataMasker = sensitiveDataMasker;
	}

	public BeneficiaryListView toListView(Beneficiary beneficiary) {
		return new BeneficiaryListView(beneficiary.getId(), beneficiary.getBeneficiaryName(),
				beneficiary.getNickname(), beneficiary.getBankName(),
				sensitiveDataMasker.maskAccountNumber(beneficiary.getAccountNumber()), beneficiary.getAccountType(),
				beneficiary.getBeneficiaryType(), beneficiary.getStatus(), beneficiary.isFavourite(),
				beneficiary.getCreatedAt());
	}

	public BeneficiaryDetailsView toDetailsView(Beneficiary beneficiary) {
		return new BeneficiaryDetailsView(beneficiary.getId(), beneficiary.getBeneficiaryName(),
				beneficiary.getNickname(), beneficiary.getBankName(), beneficiary.getBankCode(),
				beneficiary.getBranchName(), beneficiary.getBranchCode(),
				sensitiveDataMasker.maskAccountNumber(beneficiary.getAccountNumber()), beneficiary.getAccountType(),
				beneficiary.getBeneficiaryType(), beneficiary.getStatus(), beneficiary.isFavourite(),
				beneficiary.getVersion(), beneficiary.getCreatedAt(), beneficiary.getUpdatedAt());
	}

	public BeneficiaryUpdateRequest toUpdateRequest(Beneficiary beneficiary) {
		return new BeneficiaryUpdateRequest(beneficiary.getBeneficiaryName(), beneficiary.getNickname(),
				beneficiary.getBankName(), beneficiary.getBankCode(), beneficiary.getBranchName(),
				beneficiary.getBranchCode(), beneficiary.getAccountType(), beneficiary.getVersion());
	}
}
