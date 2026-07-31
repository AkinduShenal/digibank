package com.digibank.security;

import com.digibank.entity.Customer;
import com.digibank.entity.User;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import com.digibank.enums.Role;
import com.digibank.repository.BankAccountRepository;
import com.digibank.repository.CustomerRepository;
import com.digibank.repository.UserRepository;
import com.digibank.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FakeSecurityUserRepository userRepository;

	@Autowired
	private FakeSecurityCustomerRepository customerRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		userRepository.reset();
		customerRepository.reset();
	}

	@Test
	void registrationPageIsPublic() throws Exception {
		mockMvc.perform(get("/open-account"))
				.andExpect(status().isOk());
	}

	@Test
	void loginPageIsPublic() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk());
	}

	@Test
	void unauthenticatedProfileRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/profile/overview"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void customerCanLoginWithUsername() throws Exception {
		User customer = addUser("customer", "customer@example.com", Role.CUSTOMER, true, true,
				CustomerStatus.ACTIVE);

		mockMvc.perform(post("/login")
						.param("username", "customer")
						.param("password", "Password@123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/dashboard"));

		assertNotNull(userRepository.lastLoginUpdates.get(customer.getId()));
	}

	@Test
	void customerCanLoginWithEmail() throws Exception {
		addUser("customer", "customer@example.com", Role.CUSTOMER, true, true, CustomerStatus.ACTIVE);

		mockMvc.perform(post("/login")
						.param("username", "customer@example.com")
						.param("password", "Password@123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/dashboard"));
	}

	@Test
	void wrongPasswordRedirectsToError() throws Exception {
		addUser("customer", "customer@example.com", Role.CUSTOMER, true, true, CustomerStatus.ACTIVE);

		mockMvc.perform(post("/login")
						.param("username", "customer")
						.param("password", "WrongPassword@123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void disabledUserCannotLogin() throws Exception {
		addUser("customer", "customer@example.com", Role.CUSTOMER, false, true, CustomerStatus.ACTIVE);

		mockMvc.perform(post("/login")
						.param("username", "customer")
						.param("password", "Password@123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?disabled"));
	}

	@Test
	void lockedUserCannotLogin() throws Exception {
		addUser("customer", "customer@example.com", Role.CUSTOMER, true, false, CustomerStatus.ACTIVE);

		mockMvc.perform(post("/login")
						.param("username", "customer")
						.param("password", "Password@123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?locked"));
	}

	@Test
	void suspendedCustomerCannotLogin() throws Exception {
		addUser("customer", "customer@example.com", Role.CUSTOMER, true, true, CustomerStatus.SUSPENDED);

		mockMvc.perform(post("/login")
						.param("username", "customer")
						.param("password", "Password@123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?locked"));
	}

	@Test
	void roleBasedRedirectsWork() throws Exception {
		addUser("staff", "staff@example.com", Role.BANK_STAFF, true, true, CustomerStatus.ACTIVE);
		mockMvc.perform(post("/login")
						.param("username", "staff")
						.param("password", "Password@123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/staff/dashboard"));

		addUser("admin", "admin@example.com", Role.ADMIN, true, true, CustomerStatus.ACTIVE);
		mockMvc.perform(post("/login")
						.param("username", "admin")
						.param("password", "Password@123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/dashboard"));
	}

	@Test
	void logoutUsesPostAndRedirectsToLoginLogout() throws Exception {
		mockMvc.perform(post("/logout").with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?logout"));
	}

	@Test
	void loginPostRequiresCsrf() throws Exception {
		mockMvc.perform(post("/login")
						.param("username", "customer")
						.param("password", "Password@123"))
				.andExpect(status().isForbidden());
	}

	@Test
	void customerCannotActivateAccountThroughStaffEndpoint() throws Exception {
		User customer = addUser("customer", "customer@example.com", Role.CUSTOMER, true, true,
				CustomerStatus.ACTIVE);

		mockMvc.perform(post("/staff/accounts/123456789012/activate")
						.param("customerNumber", "CUScustomer")
						.with(user(new CustomUserDetails(customer, CustomerStatus.ACTIVE)))
						.with(csrf()))
				.andExpect(status().isForbidden());
	}

	private User addUser(String username, String email, Role role, boolean enabled, boolean accountNonLocked,
			CustomerStatus customerStatus) {
		User user = new User(username, email, passwordEncoder.encode("Password@123"));
		user.setRole(role);
		user.setEnabled(enabled);
		user.setAccountNonLocked(accountNonLocked);
		userRepository.saveUser(user);

		Customer customer = new Customer(user, "CUS" + username, "Test", "User", LocalDate.of(1990, 1, 1),
				Gender.OTHER, IdentityType.NATIONAL_ID, username + "123456V", "+94712345678", "Address", "Colombo");
		customer.setStatus(customerStatus);
		customerRepository.customersByUserId.put(user.getId(), customer);
		return user;
	}

	@TestConfiguration
	static class SecurityTestConfig {

		@Bean
		FakeSecurityUserRepository fakeSecurityUserRepository() {
			return new FakeSecurityUserRepository();
		}

		@Bean
		FakeSecurityCustomerRepository fakeSecurityCustomerRepository() {
			return new FakeSecurityCustomerRepository();
		}

		@Bean
		UserRepository userRepository(FakeSecurityUserRepository fakeRepository) {
			return fakeRepository.proxy();
		}

		@Bean
		CustomerRepository customerRepository(FakeSecurityCustomerRepository fakeRepository) {
			return fakeRepository.proxy();
		}

		@Bean
		BankAccountRepository bankAccountRepository() {
			InvocationHandler handler = (proxy, method, args) -> defaultValue(method.getReturnType());
			return createProxy(BankAccountRepository.class, handler);
		}

		@Bean
		AuditLogRepository auditLogRepository() {
			InvocationHandler handler = (proxy, method, args) -> defaultValue(method.getReturnType());
			return createProxy(AuditLogRepository.class, handler);
		}
	}

	static class FakeSecurityUserRepository implements InvocationHandler {

		private final Map<String, User> usersByUsername = new HashMap<>();
		private final Map<String, User> usersByEmail = new HashMap<>();
		private final Map<Long, LocalDateTime> lastLoginUpdates = new HashMap<>();
		private long nextId = 1L;

		UserRepository proxy() {
			return createProxy(UserRepository.class, this);
		}

		void reset() {
			usersByUsername.clear();
			usersByEmail.clear();
			lastLoginUpdates.clear();
			nextId = 1L;
		}

		void saveUser(User user) {
			setId(user, nextId++);
			usersByUsername.put(normalize(user.getUsername()), user);
			usersByEmail.put(normalize(user.getEmail()), user);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			return switch (method.getName()) {
				case "findByUsernameIgnoreCase" -> Optional.ofNullable(usersByUsername.get(normalize((String) args[0])));
				case "findByEmailIgnoreCase" -> Optional.ofNullable(usersByEmail.get(normalize((String) args[0])));
				case "existsByUsernameIgnoreCase" -> usersByUsername.containsKey(normalize((String) args[0]));
				case "existsByEmailIgnoreCase" -> usersByEmail.containsKey(normalize((String) args[0]));
				case "updateLastLoginAt" -> {
					lastLoginUpdates.put((Long) args[0], (LocalDateTime) args[1]);
					yield 1;
				}
				case "save" -> {
					saveUser((User) args[0]);
					yield args[0];
				}
				default -> defaultValue(method.getReturnType());
			};
		}
	}

	static class FakeSecurityCustomerRepository implements InvocationHandler {

		private final Map<Long, Customer> customersByUserId = new HashMap<>();

		CustomerRepository proxy() {
			return createProxy(CustomerRepository.class, this);
		}

		void reset() {
			customersByUserId.clear();
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			return switch (method.getName()) {
				case "findByUserId" -> Optional.ofNullable(customersByUserId.get((Long) args[0]));
				case "findByCustomerNumber", "findByIdentityNumberIgnoreCase" -> Optional.empty();
				case "existsByIdentityNumberIgnoreCase" -> false;
				case "save" -> args[0];
				default -> defaultValue(method.getReturnType());
			};
		}
	}

	private static <T> T createProxy(Class<T> type, InvocationHandler invocationHandler) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, invocationHandler));
	}

	private static String normalize(String value) {
		return value == null ? null : value.trim().toLowerCase();
	}

	private static void setId(User user, Long id) {
		try {
			Method method = User.class.getSuperclass().getDeclaredMethod("setId", Long.class);
			method.setAccessible(true);
			method.invoke(user, id);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Could not set test user id.", ex);
		}
	}

	private static Object defaultValue(Class<?> returnType) {
		if (returnType == Boolean.TYPE) {
			return false;
		}
		if (returnType == Integer.TYPE) {
			return 0;
		}
		if (returnType == Long.TYPE) {
			return 0L;
		}
		if (returnType == Void.TYPE) {
			return null;
		}
		if (returnType == Optional.class) {
			return Optional.empty();
		}
		if (returnType == List.class) {
			return List.of();
		}
		return null;
	}
}
