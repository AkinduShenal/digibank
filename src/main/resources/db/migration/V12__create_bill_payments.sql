CREATE TABLE saved_billers (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	customer_id BIGINT NOT NULL,
	provider VARCHAR(40) NOT NULL,
	nickname VARCHAR(80) NOT NULL,
	consumer_reference VARCHAR(50) NOT NULL,
	status VARCHAR(20) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT fk_saved_billers_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
	CONSTRAINT chk_saved_billers_provider CHECK (provider IN ('CEB', 'LECO', 'NWSDB', 'DIALOG_MOBILE', 'MOBITEL', 'HUTCH', 'AIRTEL', 'SLT_BROADBAND', 'DIALOG_HOME')),
	CONSTRAINT chk_saved_billers_status CHECK (status IN ('ACTIVE', 'DELETED')),
	INDEX idx_saved_billers_customer_status (customer_id, status),
	INDEX idx_saved_billers_customer_provider_reference (customer_id, provider, consumer_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bill_payments (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	customer_id BIGINT NOT NULL,
	source_account_id BIGINT NOT NULL,
	saved_biller_id BIGINT NULL,
	reference_number VARCHAR(32) NOT NULL,
	category VARCHAR(30) NOT NULL,
	provider VARCHAR(40) NOT NULL,
	consumer_reference VARCHAR(50) NOT NULL,
	biller_name_snapshot VARCHAR(120) NOT NULL,
	currency_code VARCHAR(3) NOT NULL,
	amount DECIMAL(19,2) NOT NULL,
	status VARCHAR(20) NOT NULL,
	source_balance_after DECIMAL(19,2) NOT NULL,
	description VARCHAR(140) NULL,
	paid_at DATETIME(6) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT uk_bill_payments_reference UNIQUE (reference_number),
	CONSTRAINT fk_bill_payments_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
	CONSTRAINT fk_bill_payments_source_account FOREIGN KEY (source_account_id) REFERENCES bank_accounts (id),
	CONSTRAINT fk_bill_payments_saved_biller FOREIGN KEY (saved_biller_id) REFERENCES saved_billers (id),
	CONSTRAINT chk_bill_payments_category CHECK (category IN ('ELECTRICITY', 'WATER', 'MOBILE', 'INTERNET')),
	CONSTRAINT chk_bill_payments_provider CHECK (provider IN ('CEB', 'LECO', 'NWSDB', 'DIALOG_MOBILE', 'MOBITEL', 'HUTCH', 'AIRTEL', 'SLT_BROADBAND', 'DIALOG_HOME')),
	CONSTRAINT chk_bill_payments_currency CHECK (currency_code = 'LKR'),
	CONSTRAINT chk_bill_payments_status CHECK (status IN ('COMPLETED', 'FAILED')),
	CONSTRAINT chk_bill_payments_amount CHECK (amount > 0.00),
	INDEX idx_bill_payments_customer_paid (customer_id, paid_at),
	INDEX idx_bill_payments_paid_at (paid_at),
	INDEX idx_bill_payments_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE account_transactions
	DROP CONSTRAINT chk_account_transactions_type,
	ADD CONSTRAINT chk_account_transactions_type
		CHECK (transaction_type IN ('FUND_TRANSFER', 'LOAN_DISBURSEMENT', 'LOAN_REPAYMENT', 'BILL_PAYMENT'));
