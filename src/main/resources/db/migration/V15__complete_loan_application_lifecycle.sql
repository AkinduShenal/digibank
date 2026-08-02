ALTER TABLE loan_applications
	ADD COLUMN supporting_document_reference VARCHAR(255) NULL AFTER purpose,
	ADD COLUMN cancelled_at DATETIME(6) NULL AFTER disbursed_at;

UPDATE loan_applications
SET supporting_document_reference = CONCAT('LEGACY-', application_number)
WHERE supporting_document_reference IS NULL;

ALTER TABLE loan_applications
	MODIFY supporting_document_reference VARCHAR(255) NOT NULL,
	DROP CONSTRAINT chk_loan_applications_type,
	ADD CONSTRAINT chk_loan_applications_type CHECK (loan_type IN ('PERSONAL','EDUCATION','HOME','VEHICLE','BUSINESS')),
	DROP CONSTRAINT chk_loan_applications_status,
	ADD CONSTRAINT chk_loan_applications_status CHECK (status IN ('PENDING_REVIEW','DISBURSED','REJECTED','CANCELLED','CLOSED'));
