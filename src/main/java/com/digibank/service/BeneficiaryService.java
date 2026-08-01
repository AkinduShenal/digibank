package com.digibank.service;

import com.digibank.dto.beneficiary.BeneficiaryCreateRequest;
import com.digibank.dto.beneficiary.BeneficiaryDetailsView;
import com.digibank.dto.beneficiary.BeneficiaryListView;
import com.digibank.dto.beneficiary.BeneficiaryReviewView;
import com.digibank.dto.beneficiary.BeneficiarySearchCriteria;
import com.digibank.dto.beneficiary.BeneficiaryUpdateRequest;
import com.digibank.enums.BeneficiaryVerificationStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BeneficiaryService {

	BeneficiaryDetailsView createBeneficiary(Long authenticatedUserId, BeneficiaryCreateRequest request);

	Page<BeneficiaryListView> searchBeneficiaries(Long authenticatedUserId, BeneficiarySearchCriteria criteria);

	BeneficiaryDetailsView getBeneficiary(Long authenticatedUserId, Long beneficiaryId);

	BeneficiaryDetailsView updateBeneficiary(Long authenticatedUserId, Long beneficiaryId,
			BeneficiaryUpdateRequest request);

	BeneficiaryDetailsView deactivateBeneficiary(Long authenticatedUserId, Long beneficiaryId);

	BeneficiaryDetailsView reactivateBeneficiary(Long authenticatedUserId, Long beneficiaryId);

	void deleteBeneficiary(Long authenticatedUserId, Long beneficiaryId);

	BeneficiaryDetailsView markFavourite(Long authenticatedUserId, Long beneficiaryId);

	BeneficiaryDetailsView removeFavourite(Long authenticatedUserId, Long beneficiaryId);

	List<BeneficiaryReviewView> getBeneficiariesForReview(BeneficiaryVerificationStatus verificationStatus);

	BeneficiaryDetailsView verifyBeneficiary(String actorUsername, Long beneficiaryId, String note);

	BeneficiaryDetailsView rejectBeneficiary(String actorUsername, Long beneficiaryId, String reason);
}
