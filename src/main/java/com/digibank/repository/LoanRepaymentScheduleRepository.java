package com.digibank.repository;

import com.digibank.entity.LoanRepaymentSchedule;
import com.digibank.enums.RepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentSchedule, Long> {
	List<LoanRepaymentSchedule> findByLoanApplicationIdOrderByInstallmentNumber(Long loanApplicationId);
	boolean existsByPaymentReference(String paymentReference);
	long countByLoanApplicationIdAndStatusNot(Long loanApplicationId, RepaymentStatus status);

	@Query(value = "select * from loan_repayment_schedule where loan_application_id = :loanId "
			+ "and installment_number = :installmentNumber for update", nativeQuery = true)
	Optional<LoanRepaymentSchedule> findInstallmentForUpdate(@Param("loanId") Long loanId,
			@Param("installmentNumber") int installmentNumber);

	@Query("select row from LoanRepaymentSchedule row where row.loanApplication.id = :loanId "
			+ "and row.status in :statuses order by row.installmentNumber")
	List<LoanRepaymentSchedule> findUnpaid(@Param("loanId") Long loanId,
			@Param("statuses") List<RepaymentStatus> statuses);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update LoanRepaymentSchedule row set row.status = com.digibank.enums.RepaymentStatus.OVERDUE "
			+ "where row.status = com.digibank.enums.RepaymentStatus.SCHEDULED and row.dueDate < :today")
	int markOverdue(@Param("today") LocalDate today);
}
