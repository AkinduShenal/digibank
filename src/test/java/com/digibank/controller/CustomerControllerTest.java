package com.digibank.controller;

import com.digibank.dto.customer.AccountSummaryView;
import com.digibank.dto.customer.CustomerDashboardView;
import com.digibank.dto.customer.CustomerProfileUpdateRequest;
import com.digibank.dto.customer.CustomerProfileView;
import com.digibank.entity.User;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import com.digibank.enums.Role;
import com.digibank.exception.AccountAccessDeniedException;
import com.digibank.repository.UserRepository;
import com.digibank.security.CustomAuthenticationSuccessHandler;
import com.digibank.security.CustomUserDetails;
import com.digibank.security.SecurityConfig;
import com.digibank.service.CustomerProfileService;
import com.digibank.service.AccountManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, CustomerControllerTest.TestCustomerConfig.class})
class CustomerControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FakeCustomerProfileService profileService;

	private CustomUserDetails customerUser;

	@BeforeEach
	void setUp() {
		profileService.reset();
		User user = new User("customer", "customer@example.com", "encoded-password");
		user.setRole(Role.CUSTOMER);
		setId(user, 7L);
		customerUser = new CustomUserDetails(user, CustomerStatus.ACTIVE);
	}

	@Test
	void authenticatedCustomerDashboardShowsRealModel() throws Exception {
		mockMvc.perform(get("/customer/dashboard").with(user(customerUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/dashboard"))
				.andExpect(model().attributeExists("dashboard"));
	}

	@Test
	void ownProfileAccessShowsProfile() throws Exception {
		mockMvc.perform(get("/customer/profile").with(user(customerUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/profile"))
				.andExpect(model().attributeExists("profile"));
	}

	@Test
	void unauthenticatedDashboardRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/customer/dashboard"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void profileUpdateSuccessRedirectsToProfile() throws Exception {
		mockMvc.perform(validProfileUpdate().with(user(customerUser)).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/profile"));

		assertTrue(profileService.updateCalled);
	}

	@Test
	void profileUpdateValidationFailureReturnsEditPage() throws Exception {
		mockMvc.perform(validProfileUpdate()
						.param("firstName", "A1")
						.with(user(customerUser))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/profile-edit"))
				.andExpect(model().attributeHasFieldErrors("profileUpdateRequest", "firstName"));
	}

	@Test
	void ownAccountDetailsPageLoads() throws Exception {
		mockMvc.perform(get("/customer/accounts/123456789012").with(user(customerUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("account/details"))
				.andExpect(model().attributeExists("account"));
	}

	@Test
	void otherCustomerAccountShowsSafeAccessDeniedPage() throws Exception {
		profileService.denyAccount = true;

		mockMvc.perform(get("/customer/accounts/000000000000").with(user(customerUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/access-denied"))
				.andExpect(model().attributeExists("errorMessage"));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validProfileUpdate() {
		return post("/customer/profile/edit")
				.param("firstName", "Lakshitha")
				.param("lastName", "Dilshan")
				.param("mobileNumber", "0712345678")
				.param("alternativePhone", "+94787654321")
				.param("addressLine1", "No 1 Main Street")
				.param("addressLine2", "Apartment 4")
				.param("city", "Colombo")
				.param("district", "Colombo")
				.param("province", "Western")
				.param("postalCode", "00100")
				.param("nationality", "Sri Lankan");
	}

	@TestConfiguration
	static class TestCustomerConfig {

		@Bean
		FakeCustomerProfileService customerProfileService() {
			return new FakeCustomerProfileService();
		}

		@Bean
		FakeAccountManagementService accountManagementService() {
			return new FakeAccountManagementService();
		}

		@Bean
		CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
			InvocationHandler handler = (proxy, method, args) -> 1;
			UserRepository userRepository = UserRepository.class.cast(Proxy.newProxyInstance(
					UserRepository.class.getClassLoader(), new Class<?>[] {UserRepository.class}, handler));
			return new CustomAuthenticationSuccessHandler(userRepository);
		}
	}

	static class FakeCustomerProfileService implements CustomerProfileService {

		private boolean updateCalled;
		private boolean denyAccount;

		@Override
		public CustomerDashboardView getDashboard(CustomUserDetails userDetails) {
			return new CustomerDashboardView("Lakshitha Dilshan", "CUS2026000123", "LD", null,
					LocalDateTime.of(2026, 7, 30, 14, 0), account());
		}

		@Override
		public CustomerProfileView getProfile(CustomUserDetails userDetails) {
			return new CustomerProfileView("Lakshitha Dilshan", "CUS2026000123", "******789V",
					IdentityType.NATIONAL_ID, LocalDate.of(1998, 1, 1), 28, Gender.MALE, "Sri Lankan",
					"customer@example.com", "+94712345678", "-", "No 1 Main Street, Colombo",
					CustomerStatus.ACTIVE, LocalDateTime.of(2026, 7, 30, 12, 0), "LD", null, List.of(account()));
		}

		@Override
		public CustomerProfileUpdateRequest getProfileUpdateRequest(CustomUserDetails userDetails) {
			return new CustomerProfileUpdateRequest("Lakshitha", "Dilshan", "+94712345678", "+94787654321",
					"No 1 Main Street", "Apartment 4", "Colombo", "Colombo", "Western", "00100",
					"Sri Lankan");
		}

		@Override
		public CustomerProfileView updateProfile(CustomUserDetails userDetails, CustomerProfileUpdateRequest request) {
			updateCalled = true;
			return getProfile(userDetails);
		}

		@Override
		public AccountSummaryView getAccountDetails(CustomUserDetails userDetails, String accountNumber) {
			if (denyAccount) {
				throw new AccountAccessDeniedException();
			}
			return account();
		}

		void reset() {
			updateCalled = false;
			denyAccount = false;
		}

		private AccountSummaryView account() {
			return new AccountSummaryView("123456789012", "********9012", AccountType.SAVINGS,
					AccountStatus.PENDING_ACTIVATION, CurrencyCode.LKR, new BigDecimal("1000.00"),
					new BigDecimal("1000.00"), "COL001", LocalDateTime.of(2026, 7, 30, 12, 0));
		}
	}

	static class FakeAccountManagementService implements AccountManagementService {

		@Override
		public AccountSummaryView activateAccount(String actorUsername, String accountNumber) {
			return null;
		}

		@Override
		public AccountSummaryView freezeAccount(String actorUsername, String accountNumber, String reason) {
			return null;
		}

		@Override
		public AccountSummaryView unfreezeAccount(String actorUsername, String accountNumber, String reason) {
			return null;
		}

		@Override
		public AccountSummaryView deactivateAccount(String actorUsername, String accountNumber, String reason) {
			return null;
		}

		@Override
		public AccountSummaryView closeAccount(String actorUsername, String customerNumber, String accountNumber,
				String reason) {
			return null;
		}

		@Override
		public AccountSummaryView reactivateEligibleAccount(String actorUsername, String accountNumber, String reason) {
			return null;
		}

		@Override
		public AccountSummaryView changeAccountType(String actorUsername, String accountNumber,
				com.digibank.enums.AccountType accountType, String reason) {
			return null;
		}

		@Override
		public AccountSummaryView requestAccountClosure(String actorUsername, String accountNumber, String reason) {
			return null;
		}

		@Override
		public List<com.digibank.dto.staff.StaffCustomerView> getCustomerRecords() {
			return List.of();
		}

		@Override
		public com.digibank.dto.staff.StaffCustomerView getCustomerDetails(String customerNumber) {
			return null;
		}
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
}
