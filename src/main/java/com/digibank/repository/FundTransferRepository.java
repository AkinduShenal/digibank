package com.digibank.repository;

import com.digibank.entity.FundTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FundTransferRepository extends JpaRepository<FundTransfer, Long> {

	Optional<FundTransfer> findByReferenceNumberAndCustomerId(String referenceNumber, Long customerId);

	List<FundTransfer> findTop50ByCustomerIdOrderByCreatedAtDesc(Long customerId);

	@Query(value = "select * from fund_transfers where reference_number=:reference for update", nativeQuery = true)
	Optional<FundTransfer> findByReferenceNumberForUpdate(@Param("reference") String reference);

	Optional<FundTransfer> findByReferenceNumber(String referenceNumber);

	boolean existsByReversalReference(String reversalReference);

	@Query("""
		select transfer from FundTransfer transfer
		where (:status is null or transfer.status=:status)
		and (:query is null or lower(transfer.referenceNumber) like lower(concat('%',:query,'%'))
		 or lower(transfer.customer.customerNumber) like lower(concat('%',:query,'%'))
		 or lower(transfer.beneficiaryNameSnapshot) like lower(concat('%',:query,'%')))
		""")
	Page<FundTransfer> searchForStaff(@Param("query") String query, @Param("status") com.digibank.enums.TransferStatus status,
			Pageable pageable);
}
