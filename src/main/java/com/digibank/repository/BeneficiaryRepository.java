package com.digibank.repository;

import com.digibank.entity.Beneficiary;
import com.digibank.enums.BeneficiaryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long>, JpaSpecificationExecutor<Beneficiary> {

	Optional<Beneficiary> findByIdAndCustomerId(Long id, Long customerId);

	Optional<Beneficiary> findByIdAndCustomerIdAndStatusNot(Long id, Long customerId,
			BeneficiaryStatus status);

	boolean existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNot(Long customerId,
			String bankCode, String normalizedAccountNumber, BeneficiaryStatus status);

	boolean existsByCustomerIdAndBankCodeAndNormalizedAccountNumberAndStatusNotAndIdNot(Long customerId,
			String bankCode, String normalizedAccountNumber, BeneficiaryStatus status, Long id);

	Page<Beneficiary> findByCustomerIdAndStatusNot(Long customerId, BeneficiaryStatus status,
			Pageable pageable);
}
