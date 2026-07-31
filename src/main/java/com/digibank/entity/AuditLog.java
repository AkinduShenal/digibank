package com.digibank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

	@Column(name = "actor_username", nullable = false, length = 120)
	private String actorUsername;

	@Column(name = "action", nullable = false, length = 80)
	private String action;

	@Column(name = "target_type", nullable = false, length = 40)
	private String targetType;

	@Column(name = "target_identifier", nullable = false, length = 80)
	private String targetIdentifier;

	@Column(name = "previous_status", length = 40)
	private String previousStatus;

	@Column(name = "new_status", length = 40)
	private String newStatus;

	@Column(name = "reason", length = 255)
	private String reason;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	protected AuditLog() {
	}

	public AuditLog(String actorUsername, String action, String targetType, String targetIdentifier,
			String previousStatus, String newStatus, String reason, LocalDateTime occurredAt) {
		this.actorUsername = actorUsername;
		this.action = action;
		this.targetType = targetType;
		this.targetIdentifier = targetIdentifier;
		this.previousStatus = previousStatus;
		this.newStatus = newStatus;
		this.reason = reason;
		this.occurredAt = occurredAt;
	}

	public String getActorUsername() {
		return actorUsername;
	}

	public void setActorUsername(String actorUsername) {
		this.actorUsername = actorUsername;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	public String getTargetIdentifier() {
		return targetIdentifier;
	}

	public void setTargetIdentifier(String targetIdentifier) {
		this.targetIdentifier = targetIdentifier;
	}

	public String getPreviousStatus() {
		return previousStatus;
	}

	public void setPreviousStatus(String previousStatus) {
		this.previousStatus = previousStatus;
	}

	public String getNewStatus() {
		return newStatus;
	}

	public void setNewStatus(String newStatus) {
		this.newStatus = newStatus;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public LocalDateTime getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(LocalDateTime occurredAt) {
		this.occurredAt = occurredAt;
	}
}
