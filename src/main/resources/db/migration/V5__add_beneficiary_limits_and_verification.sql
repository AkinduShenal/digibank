ALTER TABLE beneficiaries
	ADD COLUMN transfer_limit DECIMAL(19,2) NOT NULL DEFAULT 100000.00 AFTER is_favourite,
	ADD COLUMN verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER transfer_limit,
	ADD COLUMN reviewed_by VARCHAR(30) NULL AFTER verification_status,
	ADD COLUMN reviewed_at DATETIME(6) NULL AFTER reviewed_by,
	ADD COLUMN verification_note VARCHAR(255) NULL AFTER reviewed_at;

UPDATE beneficiaries
SET verification_status = 'VERIFIED',
	reviewed_by = 'SYSTEM',
	reviewed_at = CURRENT_TIMESTAMP(6),
	verification_note = 'Verified from an existing DigiBank account.'
WHERE beneficiary_type = 'INTERNAL';

ALTER TABLE beneficiaries
	ADD CONSTRAINT chk_beneficiaries_transfer_limit_positive CHECK (transfer_limit > 0.00),
	ADD CONSTRAINT chk_beneficiaries_verification_status
		CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
	ADD INDEX idx_beneficiaries_verification_status (verification_status);
