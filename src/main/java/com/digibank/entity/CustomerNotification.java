package com.digibank.entity;

import com.digibank.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_notifications")
public class CustomerNotification extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 30)
	private NotificationType notificationType;

	@Column(name = "title", nullable = false, length = 120)
	private String title;

	@Column(name = "message", nullable = false, length = 500)
	private String message;

	@Column(name = "related_reference", length = 32)
	private String relatedReference;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	protected CustomerNotification() {
	}

	public CustomerNotification(User user, NotificationType notificationType, String title, String message,
			String relatedReference) {
		this.user = user;
		this.notificationType = notificationType;
		this.title = title;
		this.message = message;
		this.relatedReference = relatedReference;
	}

	public User getUser() { return user; }
	public NotificationType getNotificationType() { return notificationType; }
	public String getTitle() { return title; }
	public String getMessage() { return message; }
	public String getRelatedReference() { return relatedReference; }
	public LocalDateTime getReadAt() { return readAt; }
	public boolean isRead() { return readAt != null; }
	public void markRead(LocalDateTime when) { this.readAt = when; }
}
