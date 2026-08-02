ALTER TABLE payment_cards
	DROP INDEX uk_payment_cards_card_number,
	MODIFY COLUMN card_number VARCHAR(255) NULL,
	ADD COLUMN card_number_hash CHAR(64) NULL AFTER card_number,
	ADD COLUMN card_last_four CHAR(4) NULL AFTER card_number_hash,
	ADD COLUMN spending_limit DECIMAL(19,2) NULL AFTER expiry_date,
	ADD COLUMN lost_stolen_at DATETIME(6) NULL AFTER blocked_at,
	ADD COLUMN cancelled_at DATETIME(6) NULL AFTER lost_stolen_at,
	ADD COLUMN cancellation_reason VARCHAR(255) NULL AFTER cancelled_at;

UPDATE payment_cards
SET spending_limit = CASE WHEN card_type = 'CREDIT' THEN 500000.00 ELSE 250000.00 END
WHERE spending_limit IS NULL;

ALTER TABLE payment_cards
	MODIFY COLUMN spending_limit DECIMAL(19,2) NOT NULL,
	ADD CONSTRAINT uk_payment_cards_number_hash UNIQUE (card_number_hash),
	DROP CONSTRAINT chk_payment_cards_status,
	ADD CONSTRAINT chk_payment_cards_status CHECK
		(status IN ('PENDING_REVIEW', 'INACTIVE', 'ACTIVE', 'BLOCKED', 'REJECTED', 'EXPIRED', 'LOST_STOLEN', 'CANCELLED'));
