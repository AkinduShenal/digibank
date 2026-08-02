CREATE TABLE password_reset_tokens (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,token_hash VARCHAR(64) NOT NULL,
	expires_at DATETIME(6) NOT NULL,used_at DATETIME(6) NULL,created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
	CONSTRAINT uk_password_reset_token_hash UNIQUE(token_hash),
	CONSTRAINT fk_password_reset_user FOREIGN KEY(user_id) REFERENCES users(id),
	INDEX idx_password_reset_user(user_id),INDEX idx_password_reset_expiry(expires_at,used_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
