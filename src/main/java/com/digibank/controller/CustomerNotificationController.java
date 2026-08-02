package com.digibank.controller;

import com.digibank.security.CustomUserDetails;
import com.digibank.service.CustomerNotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/customer/notifications", "/notifications"})
public class CustomerNotificationController {

	private final CustomerNotificationService notificationService;

	public CustomerNotificationController(CustomerNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public String notifications(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		model.addAttribute("notifications", notificationService.getNotifications(userDetails.getUserId()));
		model.addAttribute("unreadCount", notificationService.getUnreadCount(userDetails.getUserId()));
		return "customer/notifications/list";
	}

	@PostMapping("/{notificationId}/read")
	public String markRead(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long notificationId) {
		notificationService.markRead(userDetails.getUserId(), notificationId);
		return "redirect:/customer/notifications";
	}

	@PostMapping("/read-all")
	public String markAllRead(@AuthenticationPrincipal CustomUserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		notificationService.markAllRead(userDetails.getUserId());
		redirectAttributes.addFlashAttribute("successMessage", "All notifications were marked as read.");
		return "redirect:/customer/notifications";
	}
}
