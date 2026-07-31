CREATE TABLE audit_logs (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	actor_username VARCHAR(120) NOT NULL,
	action VARCHAR(80) NOT NULL,
	target_type VARCHAR(40) NOT NULL,
	target_identifier VARCHAR(80) NOT NULL,
	previous_status VARCHAR(40) NULL,
	new_status VARCHAR(40) NULL,
	reason VARCHAR(255) NULL,
	occurred_at DATETIME(6) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
