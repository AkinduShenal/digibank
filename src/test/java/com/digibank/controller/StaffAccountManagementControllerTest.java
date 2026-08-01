package com.digibank.controller;

import com.digibank.dto.beneficiary.BeneficiaryReviewView;
import com.digibank.dto.customer.AccountSummaryView;
import com.digibank.dto.staff.StaffCustomerView;
import com.digibank.entity.User;
import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryType;
import com.digibank.enums.BeneficiaryVerificationStatus;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.Role;
import com.digibank.repository.UserRepository;
import com.digibank.security.CustomAuthenticationSuccessHandler;
import com.digibank.security.CustomUserDetails;
import com.digibank.security.SecurityConfig;
import com.digibank.service.AccountManagementService;
import com.digibank.service.BeneficiaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StaffAccountManagementController.class)
@Import({SecurityConfig.class, StaffAccountManagementControllerTest.TestStaffConfig.class})
class StaffAccountManagementControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private CustomUserDetails staffUser;
	private CustomUserDetails customerUser;

	@BeforeEach
	void setUp() {
		staffUser = userDetails("staff", Role.BANK_STAFF);
		customerUser = userDetails("customer", Role.CUSTOMER);
	}

	@Test
	void staffCanOpenOperationsDashboard() throws Exception {
		mockMvc.perform(get("/staff/dashboard").with(user(staffUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("staff/dashboard"))
				.andExpect(model().attributeExists("dashboard", "recentCustomers"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Staff dashboard")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Beneficiary reviews")));
	}

	@Test
	void staffCanOpenBeneficiaryReviewQueue() throws Exception {
		mockMvc.perform(get("/staff/beneficiaries").with(user(staffUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("staff/beneficiaries"))
				.andExpect(model().attributeExists("beneficiaries", "selectedStatus", "verificationStatuses"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("CUS2026000123")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("********9012")));
	}

	@Test
	void staffCanOpenCustomerRecordAndSeeAccountActions() throws Exception {
		mockMvc.perform(get("/staff/customers/CUS2026000123").with(user(staffUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("staff/customer-details"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Verify customer & activate account")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("********9012")));
	}

	@Test
	void customerCannotOpenStaffReviewQueue() throws Exception {
		mockMvc.perform(get("/staff/beneficiaries").with(user(customerUser)))
				.andExpect(status().isForbidden())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl("/access-denied"));
	}

	@Test
	void verificationRequiresCsrf() throws Exception {
		mockMvc.perform(post("/staff/beneficiaries/40/verify").with(user(staffUser)))
				.andExpect(status().isForbidden());
	}

	@Test
	void staffCanSubmitVerification() throws Exception {
		mockMvc.perform(post("/staff/beneficiaries/40/verify")
						.param("note", "Checked")
						.with(user(staffUser))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/staff/beneficiaries"));
	}

	private CustomUserDetails userDetails(String username, Role role) {
		User user = new User(username, username + "@example.com", "encoded-password");
		user.setRole(role);
		user.setEnabled(true);
		return new CustomUserDetails(user, CustomerStatus.ACTIVE);
	}

	@TestConfiguration
	static class TestStaffConfig {

		@Bean
		AccountManagementService accountManagementService() {
			InvocationHandler handler = (proxy, method, args) -> {
				StaffCustomerView customer = customerRecord();
				if ("getCustomerRecords".equals(method.getName())) {
					return List.of(customer);
				}
				if ("getCustomerDetails".equals(method.getName())) {
					return customer;
				}
				return defaultValue(method.getReturnType());
			};
			return proxy(AccountManagementService.class, handler);
		}

		private static StaffCustomerView customerRecord() {
			AccountSummaryView account = new AccountSummaryView("123456789012", "********9012",
					AccountType.SAVINGS, AccountStatus.PENDING_ACTIVATION, CurrencyCode.LKR,
					new BigDecimal("5000.00"), new BigDecimal("5000.00"), "COL001",
					LocalDateTime.of(2026, 8, 1, 10, 0));
			return new StaffCustomerView("CUS2026000123", "Test Customer", "customer@example.com",
					"+94712345678", CustomerStatus.PENDING_VERIFICATION, false,
					LocalDateTime.of(2026, 8, 1, 10, 0), List.of(account));
		}

		@Bean
		BeneficiaryService beneficiaryService() {
			InvocationHandler handler = (proxy, method, args) -> {
				if ("getBeneficiariesForReview".equals(method.getName())) {
					return List.of(new BeneficiaryReviewView(40L, "CUS2026000123", "Test Customer",
							"Kasun Perera", "Example Bank", "EXB01", "Main", "MB01", "********9012",
							BeneficiaryAccountType.SAVINGS, BeneficiaryType.EXTERNAL,
							new BigDecimal("100000.00"), BeneficiaryVerificationStatus.PENDING, null,
							LocalDateTime.of(2026, 8, 1, 10, 0)));
				}
				return defaultValue(method.getReturnType());
			};
			return proxy(BeneficiaryService.class, handler);
		}

		@Bean
		CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
			InvocationHandler handler = (proxy, method, args) -> defaultValue(method.getReturnType());
			return new CustomAuthenticationSuccessHandler(proxy(UserRepository.class, handler));
		}

		private static <T> T proxy(Class<T> type, InvocationHandler handler) {
			return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
		}

		private static Object defaultValue(Class<?> type) {
			if (type == boolean.class) {
				return false;
			}
			if (type == int.class) {
				return 0;
			}
			if (List.class.isAssignableFrom(type)) {
				return List.of();
			}
			return null;
		}
	}
}
