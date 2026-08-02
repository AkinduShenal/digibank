CREATE TABLE loan_applications (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	customer_id BIGINT NOT NULL,
	disbursement_account_id BIGINT NOT NULL,
	application_number VARCHAR(32) NOT NULL,
	loan_type VARCHAR(30) NOT NULL,
	status VARCHAR(30) NOT NULL,
	requested_amount DECIMAL(19,2) NOT NULL,
	approved_amount DECIMAL(19,2) NULL,
	annual_interest_rate DECIMAL(7,4) NOT NULL,
	term_months INT NOT NULL,
	monthly_installment DECIMAL(19,2) NOT NULL,
	monthly_income DECIMAL(19,2) NOT NULL,
	employment_status VARCHAR(80) NOT NULL,
	purpose VARCHAR(500) NOT NULL,
	reviewed_by VARCHAR(30) NULL,
	reviewed_at DATETIME(6) NULL,
	review_note VARCHAR(500) NULL,
	disbursed_at DATETIME(6) NULL,
	version BIGINT NOT NULL DEFAULT 0,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT uk_loan_applications_number UNIQUE (application_number),
	CONSTRAINT fk_loan_applications_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
	CONSTRAINT fk_loan_applications_account FOREIGN KEY (disbursement_account_id) REFERENCES bank_accounts (id),
	CONSTRAINT chk_loan_applications_type CHECK (loan_type IN ('PERSONAL', 'EDUCATION', 'HOME', 'BUSINESS')),
	CONSTRAINT chk_loan_applications_status CHECK (status IN ('PENDING_REVIEW', 'DISBURSED', 'REJECTED', 'CLOSED')),
	CONSTRAINT chk_loan_applications_amount CHECK (requested_amount >= 50000.00 AND requested_amount <= 5000000.00),
	CONSTRAINT chk_loan_applications_term CHECK (term_months BETWEEN 6 AND 60),
	CONSTRAINT chk_loan_applications_income CHECK (monthly_income > 0.00),
	INDEX idx_loan_applications_customer_created (customer_id, created_at),
	INDEX idx_loan_applications_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE loan_repayment_schedule (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	loan_application_id BIGINT NOT NULL,
	installment_number INT NOT NULL,
	due_date DATE NOT NULL,
	principal_amount DECIMAL(19,2) NOT NULL,
	interest_amount DECIMAL(19,2) NOT NULL,
	total_amount DECIMAL(19,2) NOT NULL,
	status VARCHAR(20) NOT NULL,
	paid_at DATETIME(6) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT fk_loan_schedule_application FOREIGN KEY (loan_application_id) REFERENCES loan_applications (id),
	CONSTRAINT uk_loan_schedule_installment UNIQUE (loan_application_id, installment_number),
	CONSTRAINT chk_loan_schedule_status CHECK (status IN ('SCHEDULED', 'PAID', 'OVERDUE')),
	INDEX idx_loan_schedule_due_status (due_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE account_transactions
	DROP CONSTRAINT chk_account_transactions_type,
	ADD CONSTRAINT chk_account_transactions_type
		CHECK (transaction_type IN ('FUND_TRANSFER', 'LOAN_DISBURSEMENT', 'LOAN_REPAYMENT'));
