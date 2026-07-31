package com.digibank.controller;

import com.digibank.dto.auth.CustomerRegistrationRequest;
import com.digibank.dto.auth.RegistrationResult;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.CurrencyCode;
import com.digibank.exception.DuplicateEmailException;
import com.digibank.exception.InvalidInitialDepositException;
import com.digibank.repository.UserRepository;
import com.digibank.security.CustomAuthenticationSuccessHandler;
import com.digibank.security.SecurityConfig;
import com.digibank.service.CustomerRegistrationService;
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
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthControllerTest.TestRegistrationConfig.class})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FakeCustomerRegistrationService registrationService;

	@BeforeEach
	void setUp() {
		registrationService.reset();
	}

	@Test
	void getRegistrationPageShowsForm() throws Exception {
		mockMvc.perform(get("/open-account"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/open-account"))
				.andExpect(model().attributeExists("registrationRequest"))
				.andExpect(model().attributeExists("genderOptions"))
				.andExpect(model().attributeExists("identityTypeOptions"))
				.andExpect(model().attributeExists("accountTypeOptions"))
				.andExpect(model().attributeExists("branchOptions"));
	}

	@Test
	void validRegistrationSubmissionRedirectsWithResult() throws Exception {
		mockMvc.perform(validRegistrationPost().with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/registration-success"))
				.andExpect(flash().attribute("registrationResult", hasProperty("customerNumber", notNullValue())));

		assertTrue(registrationService.called);
	}

	@Test
	void invalidUnderageSubmissionReturnsForm() throws Exception {
		mockMvc.perform(validRegistrationPost("2012-01-01", "Password@123", "Password@123")
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/open-account"))
				.andExpect(model().attributeHasFieldErrors("registrationRequest", "dateOfBirth"));

		assertFalse(registrationService.called);
	}

	@Test
	void invalidPasswordSubmissionReturnsForm() throws Exception {
		mockMvc.perform(validRegistrationPost("1998-01-01", "weak", "weak")
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/open-account"))
				.andExpect(model().attributeHasFieldErrors("registrationRequest", "password"));

		assertFalse(registrationService.called);
	}

	@Test
	void duplicateEmailHandlingReturnsFieldError() throws Exception {
		registrationService.duplicateEmail = true;

		mockMvc.perform(validRegistrationPost().with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/open-account"))
				.andExpect(model().attributeHasFieldErrors("registrationRequest", "email"));
	}

	@Test
	void insufficientInitialDepositReturnsFieldError() throws Exception {
		registrationService.insufficientDeposit = true;

		mockMvc.perform(validRegistrationPost().with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/open-account"))
				.andExpect(model().attributeHasFieldErrors("registrationRequest", "initialDeposit"));
	}

	@Test
	void missingCsrfRejectsSubmission() throws Exception {
		mockMvc.perform(validRegistrationPost())
				.andExpect(status().isForbidden());
	}

	@Test
	void successfulRedirectGoesToSuccessPage() throws Exception {
		mockMvc.perform(validRegistrationPost().with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/registration-success"));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validRegistrationPost() {
		return validRegistrationPost("1998-01-01", "Password@123", "Password@123");
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validRegistrationPost(
			String dateOfBirth, String password, String confirmPassword) {
		return post("/open-account")
				.param("firstName", "Lakshitha")
				.param("lastName", "Dilshan")
				.param("email", "student@example.com")
				.param("mobileNumber", "0712345678")
				.param("alternativePhone", "+94787654321")
				.param("dateOfBirth", dateOfBirth)
				.param("gender", "MALE")
				.param("identityType", "NATIONAL_ID")
				.param("identityNumber", "123456789V")
				.param("nationality", "Sri Lankan")
				.param("addressLine1", "No 1 Main Street")
				.param("addressLine2", "Apartment 4")
				.param("city", "Colombo")
				.param("district", "Colombo")
				.param("province", "Western")
				.param("postalCode", "00100")
				.param("country", "Sri Lanka")
				.param("accountType", "SAVINGS")
				.param("branchCode", "COL001")
				.param("initialDeposit", "1000.00")
				.param("username", "lakshitha")
				.param("password", password)
				.param("confirmPassword", confirmPassword)
				.param("transactionPin", "2468")
				.param("confirmTransactionPin", "2468")
				.param("termsAccepted", "true")
				.param("privacyAccepted", "true");
	}

	@TestConfiguration
	static class TestRegistrationConfig {

		@Bean
		FakeCustomerRegistrationService customerRegistrationService() {
			return new FakeCustomerRegistrationService();
		}

		@Bean
		CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
			InvocationHandler handler = (proxy, method, args) -> {
				if ("updateLastLoginAt".equals(method.getName())) {
					return 1;
				}
				return defaultValue(method.getReturnType());
			};
			UserRepository userRepository = UserRepository.class.cast(Proxy.newProxyInstance(
					UserRepository.class.getClassLoader(), new Class<?>[] {UserRepository.class}, handler));
			return new CustomAuthenticationSuccessHandler(userRepository);
		}
	}

	static class FakeCustomerRegistrationService implements CustomerRegistrationService {

		private boolean called;
		private boolean duplicateEmail;
		private boolean insufficientDeposit;

		@Override
		public RegistrationResult register(CustomerRegistrationRequest request) {
			called = true;
			if (duplicateEmail) {
				throw new DuplicateEmailException(request.getEmail());
			}
			if (insufficientDeposit) {
				throw new InvalidInitialDepositException(
						"An initial deposit of at least LKR 1,000 is required for a savings account.");
			}
			return new RegistrationResult("CUS2026000123", "123456789012", AccountType.SAVINGS, CurrencyCode.LKR,
					AccountStatus.PENDING_ACTIVATION, "Lakshitha Dilshan");
		}

		void reset() {
			called = false;
			duplicateEmail = false;
			insufficientDeposit = false;
		}
	}

	private static Object defaultValue(Class<?> returnType) {
		if (returnType == Boolean.TYPE) {
			return false;
		}
		if (returnType == Integer.TYPE) {
			return 0;
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
