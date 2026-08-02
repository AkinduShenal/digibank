package com.digibank.repository;

import com.digibank.entity.ScheduledPayment;
import com.digibank.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {
	List<ScheduledPayment> findByCustomerUserIdOrderByCreatedAtDesc(Long userId);
	Optional<ScheduledPayment> findByScheduleReferenceAndCustomerUserId(String reference, Long userId);
	List<ScheduledPayment> findTop100ByStatusAndNextExecutionAtLessThanEqualOrderByNextExecutionAt(
			ScheduleStatus status, LocalDateTime dueAt);
	boolean existsByScheduleReference(String reference);
	@Query(value="select * from scheduled_payments where id=:id for update", nativeQuery=true)
	Optional<ScheduledPayment> findByIdForUpdate(@Param("id") Long id);
}
