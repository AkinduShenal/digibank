CREATE TABLE beneficiaries (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	customer_id BIGINT NOT NULL,
	beneficiary_name VARCHAR(120) NOT NULL,
	nickname VARCHAR(80) NULL,
	bank_name VARCHAR(120) NOT NULL,
	bank_code VARCHAR(20) NOT NULL,
	branch_name VARCHAR(120) NULL,
	branch_code VARCHAR(20) NULL,
	account_number VARCHAR(34) NOT NULL,
	normalized_account_number VARCHAR(34) NOT NULL,
	account_type VARCHAR(20) NOT NULL,
	beneficiary_type VARCHAR(20) NOT NULL,
	status VARCHAR(20) NOT NULL,
	is_favourite BOOLEAN NOT NULL DEFAULT FALSE,
	version BIGINT NOT NULL DEFAULT 0,
	active_duplicate_key VARCHAR(55) GENERATED ALWAYS AS (
		CASE
			WHEN status <> 'DELETED' THEN CONCAT(bank_code, ':', normalized_account_number)
			ELSE NULL
		END
	) STORED,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT fk_beneficiaries_customer_id FOREIGN KEY (customer_id) REFERENCES customers (id),
	CONSTRAINT uk_beneficiaries_customer_active_account UNIQUE (customer_id, active_duplicate_key),
	CONSTRAINT chk_beneficiaries_account_type CHECK (account_type IN ('SAVINGS', 'CURRENT')),
	CONSTRAINT chk_beneficiaries_beneficiary_type CHECK (beneficiary_type IN ('INTERNAL', 'EXTERNAL')),
	CONSTRAINT chk_beneficiaries_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
	INDEX idx_beneficiaries_customer_status (customer_id, status),
	INDEX idx_beneficiaries_customer_type (customer_id, beneficiary_type),
	INDEX idx_beneficiaries_customer_favourite (customer_id, is_favourite),
	INDEX idx_beneficiaries_normalized_account (normalized_account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
