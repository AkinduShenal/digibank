CREATE TABLE account_transactions (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	account_id BIGINT NOT NULL,
	fund_transfer_id BIGINT NULL,
	reference_number VARCHAR(32) NOT NULL,
	direction VARCHAR(10) NOT NULL,
	transaction_type VARCHAR(30) NOT NULL,
	amount DECIMAL(19,2) NOT NULL,
	balance_after DECIMAL(19,2) NULL,
	counterparty_name VARCHAR(160) NOT NULL,
	counterparty_account_masked VARCHAR(34) NOT NULL,
	description VARCHAR(140) NULL,
	occurred_at DATETIME(6) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT fk_account_transactions_account FOREIGN KEY (account_id) REFERENCES bank_accounts (id),
	CONSTRAINT fk_account_transactions_fund_transfer FOREIGN KEY (fund_transfer_id) REFERENCES fund_transfers (id),
	CONSTRAINT uk_account_transactions_transfer_account_direction
		UNIQUE (fund_transfer_id, account_id, direction),
	CONSTRAINT chk_account_transactions_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
	CONSTRAINT chk_account_transactions_type CHECK (transaction_type IN ('FUND_TRANSFER')),
	CONSTRAINT chk_account_transactions_amount_positive CHECK (amount > 0.00),
	INDEX idx_account_transactions_account_occurred (account_id, occurred_at),
	INDEX idx_account_transactions_reference (reference_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_notifications (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	user_id BIGINT NOT NULL,
	notification_type VARCHAR(30) NOT NULL,
	title VARCHAR(120) NOT NULL,
	message VARCHAR(500) NOT NULL,
	related_reference VARCHAR(32) NULL,
	read_at DATETIME(6) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT fk_customer_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
	CONSTRAINT chk_customer_notifications_type
		CHECK (notification_type IN ('INCOMING_TRANSFER', 'ACCOUNT_NOTICE')),
	INDEX idx_customer_notifications_user_created (user_id, created_at),
	INDEX idx_customer_notifications_user_unread (user_id, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO account_transactions (
	account_id, fund_transfer_id, reference_number, direction, transaction_type, amount, balance_after,
	counterparty_name, counterparty_account_masked, description, occurred_at, created_at, updated_at
)
SELECT
	transfer.source_account_id, transfer.id, transfer.reference_number, 'DEBIT', 'FUND_TRANSFER', transfer.amount,
	transfer.source_balance_after, transfer.beneficiary_name_snapshot, transfer.destination_account_masked,
	transfer.description, COALESCE(transfer.completed_at, transfer.created_at), transfer.created_at, transfer.updated_at
FROM fund_transfers transfer
WHERE transfer.status = 'COMPLETED';

INSERT INTO account_transactions (
	account_id, fund_transfer_id, reference_number, direction, transaction_type, amount, balance_after,
	counterparty_name, counterparty_account_masked, description, occurred_at, created_at, updated_at
)
SELECT
	transfer.destination_account_id, transfer.id, transfer.reference_number, 'CREDIT', 'FUND_TRANSFER', transfer.amount,
	NULL, CONCAT(sender.first_name, ' ', sender.last_name),
	CONCAT(REPEAT('*', GREATEST(CHAR_LENGTH(source_account.account_number) - 4, 0)),
		RIGHT(source_account.account_number, 4)), transfer.description,
	COALESCE(transfer.completed_at, transfer.created_at), transfer.created_at, transfer.updated_at
FROM fund_transfers transfer
JOIN customers sender ON sender.id = transfer.customer_id
JOIN bank_accounts source_account ON source_account.id = transfer.source_account_id
WHERE transfer.status = 'COMPLETED' AND transfer.destination_account_id IS NOT NULL;
