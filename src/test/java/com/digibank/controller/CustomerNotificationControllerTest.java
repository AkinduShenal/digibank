package com.digibank.controller;

import com.digibank.dto.notification.NotificationView;
import com.digibank.entity.User;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.NotificationType;
import com.digibank.enums.Role;
import com.digibank.repository.UserRepository;
import com.digibank.security.CustomAuthenticationSuccessHandler;
import com.digibank.security.CustomUserDetails;
import com.digibank.security.SecurityConfig;
import com.digibank.service.CustomerNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CustomerNotificationController.class)
@Import({SecurityConfig.class, CustomerNotificationControllerTest.NotificationTestConfig.class})
class CustomerNotificationControllerTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private FakeNotificationService notificationService;
	private CustomUserDetails customer;

	@BeforeEach
	void setUp() {
		notificationService.markedId = null;
		notificationService.markAllCalled = false;
		customer = userDetails("customer", Role.CUSTOMER, 7L);
	}

	@Test
	void notificationListRequiresLoginAndRendersIncomingTransfer() throws Exception {
		mockMvc.perform(get("/customer/notifications"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
		mockMvc.perform(get("/customer/notifications").with(user(customer)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/notifications/list"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Incoming transfer received")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("1 unread")));
	}

	@Test
	void markingNotificationReadRequiresCsrfAndUsesAuthenticatedUser() throws Exception {
		mockMvc.perform(post("/customer/notifications/50/read").with(user(customer)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/customer/notifications/50/read").with(user(customer)).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/notifications"));
		assertEquals(50L, notificationService.markedId);
	}

	@Test
	void staffCannotAccessCustomerNotificationPage() throws Exception {
		mockMvc.perform(get("/customer/notifications").with(user(userDetails("staff", Role.BANK_STAFF, 8L))))
				.andExpect(status().isForbidden());
	}

	@Test
	void legacyNotificationUrlStillRendersTheInbox() throws Exception {
		mockMvc.perform(get("/notifications").with(user(customer)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/notifications/list"));
	}

	private CustomUserDetails userDetails(String username, Role role, Long id) {
		User user = new User(username, username + "@example.com", "password");
		user.setRole(role);
		user.setEnabled(true);
		try {
			Method method = User.class.getSuperclass().getDeclaredMethod("setId", Long.class);
			method.setAccessible(true);
			method.invoke(user, id);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
		return new CustomUserDetails(user, CustomerStatus.ACTIVE);
	}

	@TestConfiguration
	static class NotificationTestConfig {
		@Bean FakeNotificationService customerNotificationService() { return new FakeNotificationService(); }
		@Bean CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
			InvocationHandler handler = (proxy, method, args) -> method.getReturnType() == int.class ? 0 : null;
			UserRepository repository = UserRepository.class.cast(Proxy.newProxyInstance(
					UserRepository.class.getClassLoader(), new Class<?>[] {UserRepository.class}, handler));
			return new CustomAuthenticationSuccessHandler(repository);
		}
	}

	static class FakeNotificationService implements CustomerNotificationService {
		private Long markedId;
		private boolean markAllCalled;
		@Override public List<NotificationView> getNotifications(Long userId) {
			return List.of(new NotificationView(50L, NotificationType.INCOMING_TRANSFER,
					"Incoming transfer received", "LKR 200.00 received from Customer.", "TRFTEST", false,
					LocalDateTime.of(2026, 8, 2, 10, 30)));
		}
		@Override public long getUnreadCount(Long userId) { return 1; }
		@Override public void markRead(Long userId, Long notificationId) { markedId = notificationId; }
		@Override public void markAllRead(Long userId) { markAllCalled = true; }
	}
}
