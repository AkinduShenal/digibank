package com.digibank.service;

import com.digibank.dto.notification.NotificationView;

import java.util.List;

public interface CustomerNotificationService {

	List<NotificationView> getNotifications(Long userId);

	long getUnreadCount(Long userId);

	void markRead(Long userId, Long notificationId);

	void markAllRead(Long userId);
}
