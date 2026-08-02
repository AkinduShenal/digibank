ALTER TABLE fund_transfers
	ADD COLUMN reversal_reference VARCHAR(32) NULL,
	ADD COLUMN reversal_reason VARCHAR(255) NULL,
	ADD COLUMN reversed_by VARCHAR(30) NULL,
	ADD COLUMN reversed_at DATETIME(6) NULL,
	ADD CONSTRAINT uk_fund_transfers_reversal_reference UNIQUE (reversal_reference);

ALTER TABLE account_transactions
	DROP CONSTRAINT chk_account_transactions_type,
	ADD CONSTRAINT chk_account_transactions_type
		CHECK (transaction_type IN ('FUND_TRANSFER', 'FUND_TRANSFER_REVERSAL', 'LOAN_DISBURSEMENT',
			'LOAN_REPAYMENT', 'BILL_PAYMENT'));
