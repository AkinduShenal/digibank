package com.digibank.repository;

import com.digibank.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
	Page<AuditLog> findByActorUsernameContainingIgnoreCaseOrActionContainingIgnoreCaseOrTargetIdentifierContainingIgnoreCase(
			String actor, String action, String target, Pageable pageable);
}
