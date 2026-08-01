package com.digibank.controller;

import com.digibank.dto.beneficiary.BeneficiaryCreateRequest;
import com.digibank.dto.beneficiary.BeneficiaryDetailsView;
import com.digibank.dto.beneficiary.BeneficiaryListView;
import com.digibank.dto.beneficiary.BeneficiaryReviewView;
import com.digibank.dto.beneficiary.BeneficiarySearchCriteria;
import com.digibank.dto.beneficiary.BeneficiaryUpdateRequest;
import com.digibank.entity.User;
import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;
import com.digibank.enums.BeneficiaryVerificationStatus;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Role;
import com.digibank.exception.BeneficiaryNotFoundException;
import com.digibank.exception.BeneficiaryVersionConflictException;
import com.digibank.exception.DuplicateBeneficiaryException;
import com.digibank.exception.InvalidBeneficiaryException;
import com.digibank.exception.InvalidBeneficiaryStateException;
import com.digibank.repository.UserRepository;
import com.digibank.security.CustomAuthenticationSuccessHandler;
import com.digibank.security.CustomUserDetails;
import com.digibank.security.SecurityConfig;
import com.digibank.service.BeneficiaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CustomerBeneficiaryController.class)
@Import({SecurityConfig.class, CustomerBeneficiaryControllerTest.TestBeneficiaryConfig.class})
class CustomerBeneficiaryControllerTest {

	private static final Long USER_ID = 7L;
	private static final Long BENEFICIARY_ID = 40L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FakeBeneficiaryService beneficiaryService;

	private CustomUserDetails customerUser;
	private CustomUserDetails staffUser;
	private CustomUserDetails adminUser;

	@BeforeEach
	void setUp() {
		beneficiaryService.reset();
		customerUser = userDetails("customer", Role.CUSTOMER, USER_ID);
		staffUser = userDetails("staff", Role.BANK_STAFF, 8L);
		adminUser = userDetails("admin", Role.ADMIN, 9L);
	}

	@Test
	void unauthenticatedListRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void authenticatedCustomerCanAccessList() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries").with(user(customerUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/beneficiaries/list"));
	}

	@Test
	void staffCannotAccessCustomerBeneficiaries() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries").with(user(staffUser)))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCannotAccessCustomerBeneficiaries() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries").with(user(adminUser)))
				.andExpect(status().isForbidden());
	}

	@Test
	void createRequiresCsrf() throws Exception {
		mockMvc.perform(validCreate().with(user(customerUser)))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateRequiresCsrf() throws Exception {
		mockMvc.perform(validUpdate().with(user(customerUser)))
				.andExpect(status().isForbidden());
	}

	@Test
	void deactivateRequiresCsrf() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/deactivate").with(user(customerUser)))
				.andExpect(status().isForbidden());
	}

	@Test
	void reactivateRequiresCsrf() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/reactivate").with(user(customerUser)))
				.andExpect(status().isForbidden());
	}

	@Test
	void deleteRequiresCsrf() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/delete").with(user(customerUser)))
				.andExpect(status().isForbidden());
	}

	@Test
	void favouriteRequiresCsrf() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/favourite").with(user(customerUser)))
				.andExpect(status().isForbidden());
	}

	@Test
	void unfavouriteRequiresCsrf() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/unfavourite").with(user(customerUser)))
				.andExpect(status().isForbidden());
	}

	@Test
	void listReturnsCorrectTemplateAndModel() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries").with(user(customerUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/beneficiaries/list"))
				.andExpect(model().attributeExists("beneficiaries", "criteria", "beneficiaryTypes",
						"beneficiaryStatuses", "pageTitle"));
	}

	@Test
	void listPassesAuthenticatedUserIdToService() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries").with(user(customerUser)));

		assertEquals(USER_ID, beneficiaryService.lastUserId);
	}

	@Test
	void listPassesSearchCriteriaToService() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries")
						.param("query", "rent")
						.param("type", "EXTERNAL")
						.param("status", "ACTIVE")
						.param("favourite", "true")
						.param("sort", "bankName")
						.param("direction", "desc")
						.with(user(customerUser)))
				.andExpect(status().isOk());

		assertEquals("rent", beneficiaryService.lastCriteria.getQuery());
		assertEquals(BeneficiaryType.EXTERNAL, beneficiaryService.lastCriteria.getType());
		assertEquals(BeneficiaryStatus.ACTIVE, beneficiaryService.lastCriteria.getStatus());
		assertEquals(Boolean.TRUE, beneficiaryService.lastCriteria.getFavourite());
		assertEquals("bankName", beneficiaryService.lastCriteria.getSort());
		assertEquals("desc", beneficiaryService.lastCriteria.getDirection());
	}

	@Test
	void statusFilterDoesNotExposeDeletedOption() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries").with(user(customerUser)))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(">DELETED<"))));
	}

	@Test
	void listModelContainsViewDtosNotEntities() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries").with(user(customerUser)))
				.andExpect(model().attribute("beneficiaries", org.hamcrest.Matchers.hasProperty("content",
						org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.instanceOf(BeneficiaryListView.class)))));
	}

	@Test
	void createFormRenders() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries/new").with(user(customerUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/beneficiaries/create"))
				.andExpect(model().attributeExists("beneficiaryCreateRequest", "beneficiaryTypes", "accountTypes"));
	}

	@Test
	void createFormRendersCsrfTokenAndInternalExternalHelp() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries/new").with(user(customerUser)))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Internal beneficiaries must be active DigiBank accounts")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("External account ownership is not verified")));
	}

	@Test
	void validCreateCallsServiceAndRedirectsToDetails() throws Exception {
		mockMvc.perform(validCreate().with(user(customerUser)).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/beneficiaries/" + BENEFICIARY_ID));

		assertTrue(beneficiaryService.createCalled);
		assertEquals(USER_ID, beneficiaryService.lastUserId);
	}

	@Test
	void createValidationErrorsReturnForm() throws Exception {
		mockMvc.perform(validCreate()
						.param("accountNumber", "")
						.with(user(customerUser))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/beneficiaries/create"))
				.andExpect(model().attributeHasFieldErrors("beneficiaryCreateRequest", "accountNumber"));
	}

	@Test
	void duplicateCreateReturnsSafeFormError() throws Exception {
		beneficiaryService.throwDuplicate = true;

		mockMvc.perform(validCreate().with(user(customerUser)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/beneficiaries/create"))
				.andExpect(model().attributeHasErrors("beneficiaryCreateRequest"))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("123456789012"))));
	}

	@Test
	void invalidInternalAccountReturnsSafeFormError() throws Exception {
		beneficiaryService.throwInvalid = true;

		mockMvc.perform(validCreate()
						.param("beneficiaryType", "INTERNAL")
						.with(user(customerUser))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/beneficiaries/create"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Internal beneficiary account is not eligible.")));
	}

	@Test
	void submittedCustomerIdIsIgnoredOnCreate() throws Exception {
		mockMvc.perform(validCreate()
						.param("customerId", "999")
						.with(user(customerUser))
						.with(csrf()))
				.andExpect(status().is3xxRedirection());

		assertEquals(USER_ID, beneficiaryService.lastUserId);
		assertFalse(hasMethod(BeneficiaryCreateRequest.class, "getCustomerId"));
	}

	@Test
	void ownerDetailsRenders() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries/40").with(user(customerUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/beneficiaries/details"))
				.andExpect(model().attributeExists("beneficiary"));
	}

	@Test
	void notFoundProducesSafe404() throws Exception {
		beneficiaryService.throwNotFound = true;

		mockMvc.perform(get("/customer/beneficiaries/999").with(user(customerUser)))
				.andExpect(status().isNotFound())
				.andExpect(view().name("customer/access-denied"));
	}

	@Test
	void detailsCallsServiceWithAuthenticatedUserId() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries/40").with(user(customerUser)));

		assertEquals(USER_ID, beneficiaryService.lastUserId);
		assertEquals(BENEFICIARY_ID, beneficiaryService.lastBeneficiaryId);
	}

	@Test
	void detailsRendersMaskedAccountOnly() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries/40").with(user(customerUser)))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("********9012")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("123456789012"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("normalizedAccountNumber"))));
	}

	@Test
	void editFormRenders() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries/40/edit").with(user(customerUser)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/beneficiaries/edit"))
				.andExpect(model().attributeExists("beneficiary", "beneficiaryUpdateRequest", "accountTypes"));
	}

	@Test
	void editFormContainsVersionAndNoAccountNumberInputBinding() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries/40/edit").with(user(customerUser)))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"version\"")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"accountNumber\""))));
	}

	@Test
	void internalEditRendersReadOnlyAuthoritativeFields() throws Exception {
		beneficiaryService.detailsView = internalDetails();

		mockMvc.perform(get("/customer/beneficiaries/40/edit").with(user(customerUser)))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("cannot be edited here")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"nickname\"")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"accountNumber\""))));
	}

	@Test
	void validExternalUpdateCallsServiceAndRedirects() throws Exception {
		mockMvc.perform(validUpdate().with(user(customerUser)).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/beneficiaries/" + BENEFICIARY_ID));

		assertTrue(beneficiaryService.updateCalled);
		assertEquals(USER_ID, beneficiaryService.lastUserId);
	}

	@Test
	void validInternalNicknameUpdateCallsService() throws Exception {
		beneficiaryService.detailsView = internalDetails();

		mockMvc.perform(post("/customer/beneficiaries/40/edit")
						.param("beneficiaryName", "Kasun Perera")
						.param("nickname", "Friend")
						.param("bankName", "DigiBank")
						.param("bankCode", "DIGIBANK")
						.param("branchCode", "COL001")
						.param("accountType", "CURRENT")
						.param("version", "1")
						.with(user(customerUser))
						.with(csrf()))
				.andExpect(status().is3xxRedirection());

		assertTrue(beneficiaryService.updateCalled);
	}

	@Test
	void invalidUpdateDtoReturnsEditForm() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/edit")
						.param("beneficiaryName", "")
						.param("nickname", "Rent")
						.param("bankName", "Example Bank")
						.param("bankCode", "EXB01")
						.param("branchName", "Main")
						.param("branchCode", "MB01")
						.param("accountType", "SAVINGS")
						.param("version", "1")
						.with(user(customerUser))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/beneficiaries/edit"))
				.andExpect(model().attributeHasFieldErrors("beneficiaryUpdateRequest", "beneficiaryName"));
	}

	@Test
	void staleVersionDisplaysConflictMessage() throws Exception {
		beneficiaryService.throwVersionConflict = true;

		mockMvc.perform(validUpdate().with(user(customerUser)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/beneficiaries/edit"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("updated in another session")));
	}

	@Test
	void duplicateUpdateDisplaysSafeError() throws Exception {
		beneficiaryService.throwDuplicate = true;

		mockMvc.perform(validUpdate().with(user(customerUser)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/beneficiaries/edit"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("already exists")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("123456789012"))));
	}

	@Test
	void accountNumberAndBeneficiaryTypeAreNotBindableThroughUpdateDto() {
		assertFalse(hasMethod(BeneficiaryUpdateRequest.class, "getAccountNumber"));
		assertFalse(hasMethod(BeneficiaryUpdateRequest.class, "getBeneficiaryType"));
	}

	@Test
	void deactivateCallsService() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/deactivate").with(user(customerUser)).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/beneficiaries/40"));

		assertEquals("deactivate", beneficiaryService.lastAction);
	}

	@Test
	void reactivateCallsService() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/reactivate").with(user(customerUser)).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/beneficiaries/40"));

		assertEquals("reactivate", beneficiaryService.lastAction);
	}

	@Test
	void deleteCallsSoftDeleteService() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/delete").with(user(customerUser)).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/beneficiaries"));

		assertEquals("delete", beneficiaryService.lastAction);
	}

	@Test
	void invalidTransitionProducesSafeFlashError() throws Exception {
		beneficiaryService.throwInvalidState = true;

		mockMvc.perform(post("/customer/beneficiaries/40/deactivate").with(user(customerUser)).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attributeExists("errorMessage"));
	}

	@Test
	void mutationRedirectsSafelyWithoutReturnUrl() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/favourite")
						.param("returnUrl", "https://evil.example")
						.with(user(customerUser))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/beneficiaries/40"));
	}

	@Test
	void favouriteCallsService() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/favourite").with(user(customerUser)).with(csrf()))
				.andExpect(status().is3xxRedirection());

		assertEquals("favourite", beneficiaryService.lastAction);
	}

	@Test
	void unfavouriteCallsService() throws Exception {
		mockMvc.perform(post("/customer/beneficiaries/40/unfavourite").with(user(customerUser)).with(csrf()))
				.andExpect(status().is3xxRedirection());

		assertEquals("unfavourite", beneficiaryService.lastAction);
	}

	@Test
	void inactiveFavouriteFailureIsHandledSafely() throws Exception {
		beneficiaryService.throwInvalidState = true;

		mockMvc.perform(post("/customer/beneficiaries/40/favourite").with(user(customerUser)).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attributeExists("errorMessage"));
	}

	@Test
	void listPageRendersMaskedAccountAndNoFullAccountNumber() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries").with(user(customerUser)))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("********9012")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("123456789012"))));
	}

	@Test
	void deletedRecordsAreNotRenderedFromNormalList() throws Exception {
		mockMvc.perform(get("/customer/beneficiaries").with(user(customerUser)))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("DELETED"))));
	}

	@Test
	void errorsDoNotExposeSqlDetails() throws Exception {
		beneficiaryService.throwDuplicate = true;

		mockMvc.perform(validCreate().with(user(customerUser)).with(csrf()))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("uk_beneficiaries"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL"))));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validCreate() {
		return post("/customer/beneficiaries")
				.param("beneficiaryType", "EXTERNAL")
				.param("beneficiaryName", "Kasun Perera")
				.param("nickname", "Rent")
				.param("bankName", "Example Bank")
				.param("bankCode", "EXB01")
				.param("branchName", "Main")
				.param("branchCode", "MB01")
				.param("accountNumber", "123 456-789012")
				.param("accountType", "SAVINGS");
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validUpdate() {
		return post("/customer/beneficiaries/40/edit")
				.param("beneficiaryName", "Kasun Perera")
				.param("nickname", "Rent")
				.param("bankName", "Example Bank")
				.param("bankCode", "EXB01")
				.param("branchName", "Main")
				.param("branchCode", "MB01")
				.param("accountType", "SAVINGS")
				.param("version", "1");
	}

	private CustomUserDetails userDetails(String username, Role role, Long id) {
		User user = new User(username, username + "@example.com", "encoded-password");
		user.setRole(role);
		setId(user, id);
		return new CustomUserDetails(user, CustomerStatus.ACTIVE);
	}

	private boolean hasMethod(Class<?> type, String methodName) {
		for (Method method : type.getMethods()) {
			if (methodName.equals(method.getName())) {
				return true;
			}
		}
		return false;
	}

	private static void setId(Object entity, Long id) {
		try {
			Method method = entity.getClass().getSuperclass().getDeclaredMethod("setId", Long.class);
			method.setAccessible(true);
			method.invoke(entity, id);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static BeneficiaryDetailsView externalDetails() {
		return new BeneficiaryDetailsView(BENEFICIARY_ID, "Kasun Perera", "Rent", "Example Bank",
				"EXB01", "Main", "MB01", "********9012", BeneficiaryAccountType.SAVINGS,
				BeneficiaryType.EXTERNAL, BeneficiaryStatus.ACTIVE, false, 1L,
				LocalDateTime.of(2026, 7, 31, 9, 0), LocalDateTime.of(2026, 7, 31, 9, 30));
	}

	private static BeneficiaryDetailsView internalDetails() {
		return new BeneficiaryDetailsView(BENEFICIARY_ID, "Kasun Perera", "Friend", "DigiBank",
				"DIGIBANK", null, "COL001", "********9012", BeneficiaryAccountType.CURRENT,
				BeneficiaryType.INTERNAL, BeneficiaryStatus.ACTIVE, false, 1L,
				LocalDateTime.of(2026, 7, 31, 9, 0), LocalDateTime.of(2026, 7, 31, 9, 30));
	}

	@TestConfiguration
	static class TestBeneficiaryConfig {

		@Bean
		FakeBeneficiaryService beneficiaryService() {
			return new FakeBeneficiaryService();
		}

		@Bean
		CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
			InvocationHandler handler = (proxy, method, args) -> 1;
			UserRepository userRepository = UserRepository.class.cast(Proxy.newProxyInstance(
					UserRepository.class.getClassLoader(), new Class<?>[] {UserRepository.class}, handler));
			return new CustomAuthenticationSuccessHandler(userRepository);
		}
	}

	static class FakeBeneficiaryService implements BeneficiaryService {

		private Long lastUserId;
		private Long lastBeneficiaryId;
		private BeneficiarySearchCriteria lastCriteria;
		private boolean createCalled;
		private boolean updateCalled;
		private boolean throwDuplicate;
		private boolean throwInvalid;
		private boolean throwInvalidState;
		private boolean throwVersionConflict;
		private boolean throwNotFound;
		private String lastAction;
		private BeneficiaryDetailsView detailsView = externalDetails();

		@Override
		public BeneficiaryDetailsView createBeneficiary(Long authenticatedUserId, BeneficiaryCreateRequest request) {
			lastUserId = authenticatedUserId;
			createCalled = true;
			throwConfigured();
			return detailsView;
		}

		@Override
		public Page<BeneficiaryListView> searchBeneficiaries(Long authenticatedUserId,
				BeneficiarySearchCriteria criteria) {
			lastUserId = authenticatedUserId;
			lastCriteria = criteria;
			BeneficiaryListView view = new BeneficiaryListView(BENEFICIARY_ID, "Kasun Perera", "Rent",
					"Example Bank", "********9012", BeneficiaryAccountType.SAVINGS,
					BeneficiaryType.EXTERNAL, BeneficiaryStatus.ACTIVE, false,
					LocalDateTime.of(2026, 7, 31, 9, 0));
			return new PageImpl<>(List.of(view), Pageable.ofSize(10), 1);
		}

		@Override
		public BeneficiaryDetailsView getBeneficiary(Long authenticatedUserId, Long beneficiaryId) {
			lastUserId = authenticatedUserId;
			lastBeneficiaryId = beneficiaryId;
			if (throwNotFound) {
				throw new BeneficiaryNotFoundException();
			}
			return detailsView;
		}

		@Override
		public BeneficiaryDetailsView updateBeneficiary(Long authenticatedUserId, Long beneficiaryId,
				BeneficiaryUpdateRequest request) {
			lastUserId = authenticatedUserId;
			lastBeneficiaryId = beneficiaryId;
			updateCalled = true;
			if (throwVersionConflict) {
				throw new BeneficiaryVersionConflictException();
			}
			throwConfigured();
			return detailsView;
		}

		@Override
		public BeneficiaryDetailsView deactivateBeneficiary(Long authenticatedUserId, Long beneficiaryId) {
			return action(authenticatedUserId, beneficiaryId, "deactivate");
		}

		@Override
		public BeneficiaryDetailsView reactivateBeneficiary(Long authenticatedUserId, Long beneficiaryId) {
			return action(authenticatedUserId, beneficiaryId, "reactivate");
		}

		@Override
		public void deleteBeneficiary(Long authenticatedUserId, Long beneficiaryId) {
			lastUserId = authenticatedUserId;
			lastBeneficiaryId = beneficiaryId;
			lastAction = "delete";
			throwConfigured();
		}

		@Override
		public BeneficiaryDetailsView markFavourite(Long authenticatedUserId, Long beneficiaryId) {
			return action(authenticatedUserId, beneficiaryId, "favourite");
		}

		@Override
		public BeneficiaryDetailsView removeFavourite(Long authenticatedUserId, Long beneficiaryId) {
			return action(authenticatedUserId, beneficiaryId, "unfavourite");
		}

		@Override
		public List<BeneficiaryReviewView> getBeneficiariesForReview(
				BeneficiaryVerificationStatus verificationStatus) {
			return List.of();
		}

		@Override
		public BeneficiaryDetailsView verifyBeneficiary(String actorUsername, Long beneficiaryId, String note) {
			lastBeneficiaryId = beneficiaryId;
			lastAction = "verify";
			return detailsView;
		}

		@Override
		public BeneficiaryDetailsView rejectBeneficiary(String actorUsername, Long beneficiaryId, String reason) {
			lastBeneficiaryId = beneficiaryId;
			lastAction = "reject";
			return detailsView;
		}

		void reset() {
			lastUserId = null;
			lastBeneficiaryId = null;
			lastCriteria = null;
			createCalled = false;
			updateCalled = false;
			throwDuplicate = false;
			throwInvalid = false;
			throwInvalidState = false;
			throwVersionConflict = false;
			throwNotFound = false;
			lastAction = null;
			detailsView = externalDetails();
		}

		private BeneficiaryDetailsView action(Long authenticatedUserId, Long beneficiaryId, String action) {
			lastUserId = authenticatedUserId;
			lastBeneficiaryId = beneficiaryId;
			lastAction = action;
			throwConfigured();
			return detailsView;
		}

		private void throwConfigured() {
			if (throwDuplicate) {
				throw new DuplicateBeneficiaryException();
			}
			if (throwInvalid) {
				throw new InvalidBeneficiaryException("Internal beneficiary account is not eligible.");
			}
			if (throwInvalidState) {
				throw new InvalidBeneficiaryStateException("Only active beneficiaries can be marked as favourite.");
			}
		}
	}
}
