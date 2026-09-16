package com.digibank.repository;

import com.digibank.entity.ScheduledPayment;
import com.digibank.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {
	List<ScheduledPayment> findByCustomerUserIdOrderByCreatedAtDesc(Long userId);
	List<ScheduledPayment> findByCustomerUserIdAndPaymentTypeOrderByCreatedAtDesc(Long userId,
			com.digibank.enums.ScheduledPaymentType type);
	List<ScheduledPayment> findByCustomerUserIdAndPaymentTypeAndStatusNotOrderByCreatedAtDesc(Long userId,
			com.digibank.enums.ScheduledPaymentType type, ScheduleStatus status);
	@Query(value="select s.* from scheduled_payments s where s.schedule_reference=:reference "
			+ "and s.customer_id in (select c.id from customers c where c.user_id=:userId) for update", nativeQuery=true)
	Optional<ScheduledPayment> findOwnedForUpdate(@Param("reference") String reference, @Param("userId") Long userId);
	Optional<ScheduledPayment> findByScheduleReferenceAndCustomerUserId(String reference, Long userId);
	List<ScheduledPayment> findTop100ByStatusAndNextExecutionAtLessThanEqualOrderByNextExecutionAt(
			ScheduleStatus status, LocalDateTime dueAt);
	boolean existsByScheduleReference(String reference);
	@Query(value="select * from scheduled_payments where id=:id for update", nativeQuery=true)
	Optional<ScheduledPayment> findByIdForUpdate(@Param("id") Long id);
}
