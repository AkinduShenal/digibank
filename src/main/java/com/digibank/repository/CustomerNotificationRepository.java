package com.digibank.repository;

import com.digibank.entity.CustomerNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, Long> {

	List<CustomerNotification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

	long countByUserIdAndReadAtIsNull(Long userId);

	Optional<CustomerNotification> findByIdAndUserId(Long id, Long userId);

	List<CustomerNotification> findByUserIdAndReadAtIsNull(Long userId);
}
