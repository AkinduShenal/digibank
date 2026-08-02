ALTER TABLE loan_applications
	ADD COLUMN supporting_document_stored_name VARCHAR(100) NULL,
	ADD COLUMN supporting_document_content_type VARCHAR(100) NULL,
	ADD COLUMN supporting_document_size BIGINT NULL;
