package com.digibank.repository;

import com.digibank.entity.FundTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FundTransferRepository extends JpaRepository<FundTransfer, Long> {

	Optional<FundTransfer> findByReferenceNumberAndCustomerId(String referenceNumber, Long customerId);

	List<FundTransfer> findTop50ByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
