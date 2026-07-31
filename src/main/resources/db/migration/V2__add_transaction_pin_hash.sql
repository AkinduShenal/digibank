ALTER TABLE users
	ADD COLUMN transaction_pin_hash VARCHAR(255) NOT NULL AFTER password_hash;
