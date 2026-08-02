package com.digibank.dto.notification;

import com.digibank.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationView(Long id, NotificationType type, String title, String message,
		String relatedReference, boolean read, LocalDateTime createdAt) {
}
