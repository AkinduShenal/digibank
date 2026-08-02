package com.digibank.repository;

import com.digibank.entity.LoanApplication;
import com.digibank.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
	List<LoanApplication> findByCustomerUserIdOrderByCreatedAtDesc(Long userId);
	Optional<LoanApplication> findByApplicationNumberAndCustomerUserId(String applicationNumber, Long userId);
	Optional<LoanApplication> findByApplicationNumber(String applicationNumber);
	List<LoanApplication> findByStatusOrderByCreatedAtAsc(LoanStatus status);
	boolean existsByApplicationNumber(String applicationNumber);
	boolean existsByCustomerIdAndStatus(Long customerId, LoanStatus status);

	@Query(value = "select * from loan_applications where application_number = :applicationNumber for update",
			nativeQuery = true)
	Optional<LoanApplication> findByApplicationNumberForUpdate(@Param("applicationNumber") String applicationNumber);
}
