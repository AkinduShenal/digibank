package com.digibank.repository;

import com.digibank.entity.BillPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {
	List<BillPayment> findTop50ByCustomerIdOrderByPaidAtDesc(Long customerId);
	Optional<BillPayment> findByReferenceNumberAndCustomerId(String referenceNumber, Long customerId);
	Optional<BillPayment> findByReferenceNumber(String referenceNumber);
	List<BillPayment> findTop100ByOrderByPaidAtDesc();
	boolean existsByReferenceNumber(String referenceNumber);
}
