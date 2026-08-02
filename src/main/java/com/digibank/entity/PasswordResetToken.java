package com.digibank.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="password_reset_tokens",uniqueConstraints=@UniqueConstraint(name="uk_password_reset_token_hash",columnNames="token_hash"))
public class PasswordResetToken extends BaseEntity {
	@ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
	@Column(name="token_hash",nullable=false,length=64) private String tokenHash;
	@Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
	@Column(name="used_at") private LocalDateTime usedAt;
	protected PasswordResetToken(){}
	public PasswordResetToken(User user,String tokenHash,LocalDateTime expiresAt){this.user=user;this.tokenHash=tokenHash;this.expiresAt=expiresAt;}
	public boolean usableAt(LocalDateTime now){return usedAt==null&&expiresAt.isAfter(now);}
	public void markUsed(LocalDateTime when){usedAt=when;}
	public User getUser(){return user;} public String getTokenHash(){return tokenHash;} public LocalDateTime getExpiresAt(){return expiresAt;} public LocalDateTime getUsedAt(){return usedAt;}
}
