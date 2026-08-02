package com.digibank.service.impl;

import com.digibank.dto.notification.NotificationView;
import com.digibank.entity.CustomerNotification;
import com.digibank.repository.CustomerNotificationRepository;
import com.digibank.service.CustomerNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerNotificationServiceImpl implements CustomerNotificationService {

	private final CustomerNotificationRepository notificationRepository;

	public CustomerNotificationServiceImpl(CustomerNotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<NotificationView> getNotifications(Long userId) {
		return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(requireUserId(userId)).stream()
				.map(this::view)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public long getUnreadCount(Long userId) {
		return notificationRepository.countByUserIdAndReadAtIsNull(requireUserId(userId));
	}

	@Override
	@Transactional
	public void markRead(Long userId, Long notificationId) {
		if (notificationId == null) {
			return;
		}
		notificationRepository.findByIdAndUserId(notificationId, requireUserId(userId)).ifPresent(notification -> {
			if (!notification.isRead()) {
				notification.markRead(LocalDateTime.now());
				notificationRepository.save(notification);
			}
		});
	}

	@Override
	@Transactional
	public void markAllRead(Long userId) {
		List<CustomerNotification> unread = notificationRepository.findByUserIdAndReadAtIsNull(requireUserId(userId));
		LocalDateTime now = LocalDateTime.now();
		unread.forEach(notification -> notification.markRead(now));
		notificationRepository.saveAll(unread);
	}

	private NotificationView view(CustomerNotification notification) {
		return new NotificationView(notification.getId(), notification.getNotificationType(), notification.getTitle(),
				notification.getMessage(), notification.getRelatedReference(), notification.isRead(),
				notification.getCreatedAt());
	}

	private Long requireUserId(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("Authenticated user is required.");
		}
		return userId;
	}
}
