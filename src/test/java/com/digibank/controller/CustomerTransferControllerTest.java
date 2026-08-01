package com.digibank.controller;

import com.digibank.dto.transfer.InternalAccountLookupView;
import com.digibank.dto.transfer.TransferAccountOption;
import com.digibank.dto.transfer.TransferBeneficiaryOption;
import com.digibank.dto.transfer.TransferDetailsView;
import com.digibank.dto.transfer.TransferFormView;
import com.digibank.dto.transfer.TransferListView;
import com.digibank.dto.transfer.TransferRequest;
import com.digibank.entity.User;
import com.digibank.enums.AccountType;
import com.digibank.enums.BeneficiaryType;
import com.digibank.enums.CurrencyCode;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Role;
import com.digibank.enums.TransferStatus;
import com.digibank.enums.TransferType;
import com.digibank.exception.TransferException;
import com.digibank.repository.UserRepository;
import com.digibank.security.CustomAuthenticationSuccessHandler;
import com.digibank.security.CustomUserDetails;
import com.digibank.security.SecurityConfig;
import com.digibank.service.TransferService;
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

@WebMvcTest(CustomerTransferController.class)
@Import({SecurityConfig.class, CustomerTransferControllerTest.TransferTestConfig.class})
class CustomerTransferControllerTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private FakeTransferService transferService;

	private CustomUserDetails customer;
	private CustomUserDetails staff;

	@BeforeEach
	void setUp() {
		transferService.failTransfer = false;
		customer = userDetails("customer", Role.CUSTOMER, 7L);
		staff = userDetails("staff", Role.BANK_STAFF, 8L);
	}

	@Test
	void unauthenticatedTransferFormRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/customer/transfers/new"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void staffCannotAccessCustomerTransferForm() throws Exception {
		mockMvc.perform(get("/customer/transfers/new").with(user(staff)))
				.andExpect(status().isForbidden())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl("/access-denied"));
	}

	@Test
	void customerTransferFormRendersSecureOptions() throws Exception {
		mockMvc.perform(get("/customer/transfers/new").with(user(customer)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/transfers/new"))
				.andExpect(model().attributeExists("transferRequest", "transferForm"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("********3333")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Verified Receiver")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("DigiBank account number")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
	}

	@Test
	void internalAccountLookupRequiresCustomerAndCsrf() throws Exception {
		mockMvc.perform(post("/customer/transfers/internal-account-lookup")
				.param("accountNumber", "999988887777").with(user(customer)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/customer/transfers/internal-account-lookup")
				.param("accountNumber", "999988887777").with(user(staff)).with(csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void customerCanResolveExactInternalAccount() throws Exception {
		mockMvc.perform(post("/customer/transfers/internal-account-lookup")
				.param("accountNumber", "999988887777").with(user(customer)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"available\":true,\"accountHolderName\":\"Direct Receiver\","
						+ "\"maskedAccountNumber\":\"********7777\"}"));
	}

	@Test
	void transferSubmissionRequiresCsrf() throws Exception {
		mockMvc.perform(validTransfer().with(user(customer)))
				.andExpect(status().isForbidden());
	}

	@Test
	void invalidTransferReturnsForm() throws Exception {
		mockMvc.perform(post("/customer/transfers")
				.param("sourceAccountNumber", "111122223333")
				.param("beneficiaryId", "40")
				.param("amount", "0")
				.param("description", "Invoice 100")
				.param("transactionPin", "1234")
				.with(user(customer)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/transfers/new"))
				.andExpect(model().attributeHasFieldErrors("transferRequest", "amount"));
	}

	@Test
	void validTransferRedirectsToReceipt() throws Exception {
		mockMvc.perform(validTransfer().with(user(customer)).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/transfers/TRFTEST123"));
	}

	@Test
	void directInternalTransferRedirectsToReceipt() throws Exception {
		mockMvc.perform(post("/customer/transfers")
				.param("sourceAccountNumber", "111122223333")
				.param("recipientType", "DIGIBANK_ACCOUNT")
				.param("destinationAccountNumber", "999988887777")
				.param("amount", "100.00")
				.param("description", "Direct payment")
				.param("transactionPin", "1234")
				.with(user(customer)).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/transfers/TRFTEST123"));
	}

	@Test
	void businessValidationFailureReturnsFormAndSafeMessage() throws Exception {
		transferService.failTransfer = true;

		mockMvc.perform(validTransfer().with(user(customer)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/transfers/new"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Insufficient account balance")));
	}

	@Test
	void customerCanViewHistoryAndReceipt() throws Exception {
		mockMvc.perform(get("/customer/transfers").with(user(customer)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/transfers/history"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("TRFTEST123")));

		mockMvc.perform(get("/customer/transfers/TRFTEST123").with(user(customer)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/transfers/details"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Transfer complete")));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validTransfer() {
		return post("/customer/transfers")
				.param("sourceAccountNumber", "111122223333")
				.param("recipientType", "SAVED_BENEFICIARY")
				.param("beneficiaryId", "40")
				.param("amount", "100.00")
				.param("description", "Invoice 100")
				.param("transactionPin", "1234");
	}

	private CustomUserDetails userDetails(String username, Role role, Long id) {
		User user = new User(username, username + "@example.com", "password");
		user.setRole(role);
		user.setEnabled(true);
		setId(user, id);
		return new CustomUserDetails(user, CustomerStatus.ACTIVE);
	}

	private void setId(User user, Long id) {
		try {
			var method = User.class.getSuperclass().getDeclaredMethod("setId", Long.class);
			method.setAccessible(true);
			method.invoke(user, id);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@TestConfiguration
	static class TransferTestConfig {
		@Bean FakeTransferService transferService() { return new FakeTransferService(); }

		@Bean
		CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
			InvocationHandler handler = (proxy, method, args) -> method.getReturnType() == int.class ? 0 : null;
			UserRepository repository = UserRepository.class.cast(Proxy.newProxyInstance(
					UserRepository.class.getClassLoader(), new Class<?>[] {UserRepository.class}, handler));
			return new CustomAuthenticationSuccessHandler(repository);
		}
	}

	static class FakeTransferService implements TransferService {
		private boolean failTransfer;

		@Override
		public TransferFormView getTransferForm(Long authenticatedUserId) {
			return new TransferFormView(
					List.of(new TransferAccountOption("111122223333", "********3333", AccountType.SAVINGS,
							new BigDecimal("1000.00"))),
					List.of(new TransferBeneficiaryOption(40L, "Verified Receiver", "DigiBank", "********7777",
							BeneficiaryType.INTERNAL, new BigDecimal("500.00"))));
		}

		@Override
		public InternalAccountLookupView lookupInternalAccount(Long authenticatedUserId, String accountNumber) {
			return new InternalAccountLookupView(true, "Direct Receiver", "********7777");
		}

		@Override
		public TransferDetailsView transfer(Long authenticatedUserId, String actorUsername, TransferRequest request) {
			if (failTransfer) {
				throw new TransferException("Insufficient account balance for this transfer.");
			}
			return details();
		}

		@Override
		public List<TransferListView> getTransferHistory(Long authenticatedUserId) {
			return List.of(new TransferListView("TRFTEST123", "Verified Receiver", "DigiBank", "********7777",
					new BigDecimal("100.00"), CurrencyCode.LKR, TransferType.INTERNAL, TransferStatus.COMPLETED,
					LocalDateTime.of(2026, 8, 1, 12, 0)));
		}

		@Override
		public TransferDetailsView getTransfer(Long authenticatedUserId, String referenceNumber) {
			return details();
		}

		private TransferDetailsView details() {
			return new TransferDetailsView("TRFTEST123", "********3333", "Verified Receiver", "DigiBank",
					"********7777", new BigDecimal("100.00"), CurrencyCode.LKR, TransferType.INTERNAL,
					TransferStatus.COMPLETED, "Invoice 100", new BigDecimal("900.00"),
					LocalDateTime.of(2026, 8, 1, 12, 0), LocalDateTime.of(2026, 8, 1, 12, 0));
		}
	}
}
