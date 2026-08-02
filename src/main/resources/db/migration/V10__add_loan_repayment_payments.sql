ALTER TABLE loan_repayment_schedule
	ADD COLUMN payment_reference VARCHAR(32) NULL AFTER status,
	ADD COLUMN paid_from_account_id BIGINT NULL AFTER payment_reference,
	ADD COLUMN balance_after DECIMAL(19,2) NULL AFTER paid_from_account_id,
	ADD CONSTRAINT uk_loan_schedule_payment_reference UNIQUE (payment_reference),
	ADD CONSTRAINT fk_loan_schedule_paid_account FOREIGN KEY (paid_from_account_id) REFERENCES bank_accounts (id),
	ADD INDEX idx_loan_schedule_payment_reference (payment_reference),
	ADD INDEX idx_loan_schedule_loan_status (loan_application_id, status);
