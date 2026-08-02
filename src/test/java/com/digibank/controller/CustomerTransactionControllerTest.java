package com.digibank.controller;

import com.digibank.dto.transaction.StatementAccountOption;
import com.digibank.dto.transaction.StatementExport;
import com.digibank.dto.transaction.TransactionSearchCriteria;
import com.digibank.dto.transaction.TransactionStatementView;
import com.digibank.dto.transaction.TransactionView;
import com.digibank.entity.User;
import com.digibank.enums.AccountTransactionType;
import com.digibank.enums.AccountType;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Role;
import com.digibank.enums.TransactionDirection;
import com.digibank.exception.TransactionRecordNotFoundException;
import com.digibank.repository.UserRepository;
import com.digibank.security.CustomAuthenticationSuccessHandler;
import com.digibank.security.CustomUserDetails;
import com.digibank.security.SecurityConfig;
import com.digibank.service.AccountTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CustomerTransactionController.class)
@Import({SecurityConfig.class, CustomerTransactionControllerTest.TransactionTestConfig.class})
class CustomerTransactionControllerTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private FakeTransactionService transactionService;

	private CustomUserDetails customer;
	private CustomUserDetails staff;

	@BeforeEach
	void setUp() {
		transactionService.missing = false;
		customer = userDetails("customer", Role.CUSTOMER, 7L);
		staff = userDetails("staff", Role.BANK_STAFF, 8L);
	}

	@Test
	void unauthenticatedStatementRedirectsToLoginAndStaffIsForbidden() throws Exception {
		mockMvc.perform(get("/customer/transactions"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
		mockMvc.perform(get("/customer/transactions").with(user(staff)))
				.andExpect(status().isForbidden());
	}

	@Test
	void customerStatementRendersFiltersAndDebitCreditEntries() throws Exception {
		mockMvc.perform(get("/customer/transactions").with(user(customer)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/transactions/statement"))
				.andExpect(model().attributeExists("criteria", "statement"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Transaction statement")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Download CSV")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("TRFTEST")));
	}

	@Test
	void customerCanViewOwnedTransactionDetails() throws Exception {
		mockMvc.perform(get("/customer/transactions/40").with(user(customer)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/transactions/details"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Receiver Customer")));
	}

	@Test
	void unavailableTransactionUsesSafeAccessDeniedPage() throws Exception {
		transactionService.missing = true;
		mockMvc.perform(get("/customer/transactions/99").with(user(customer)))
				.andExpect(status().isOk())
				.andExpect(view().name("customer/access-denied"));
	}

	@Test
	void csvAndPdfExportsHaveSafeDownloadHeaders() throws Exception {
		mockMvc.perform(get("/customer/transactions/export/csv").with(user(customer)))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Reference")));
		mockMvc.perform(get("/customer/transactions/export/pdf").with(user(customer)))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"))
				.andExpect(content().bytes("%PDF-1.4\n%%EOF".getBytes(StandardCharsets.ISO_8859_1)));
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
	static class TransactionTestConfig {
		@Bean FakeTransactionService accountTransactionService() { return new FakeTransactionService(); }
		@Bean CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
			InvocationHandler handler = (proxy, method, args) -> method.getReturnType() == int.class ? 0 : null;
			UserRepository repository = UserRepository.class.cast(Proxy.newProxyInstance(
					UserRepository.class.getClassLoader(), new Class<?>[] {UserRepository.class}, handler));
			return new CustomAuthenticationSuccessHandler(repository);
		}
	}

	static class FakeTransactionService implements AccountTransactionService {
		private boolean missing;

		@Override
		public TransactionStatementView getStatement(Long userId, TransactionSearchCriteria criteria) {
			return new TransactionStatementView(
					List.of(new StatementAccountOption("111122223333", "********3333", AccountType.SAVINGS)),
					new PageImpl<>(List.of(transaction()), PageRequest.of(0, 15), 1));
		}
		@Override
		public TransactionView getTransaction(Long userId, Long transactionId) {
			if (missing) throw new TransactionRecordNotFoundException();
			return transaction();
		}
		@Override public List<TransactionView> getRecentTransactions(Long userId, int limit) { return List.of(transaction()); }
		@Override public StatementExport exportCsv(Long userId, TransactionSearchCriteria criteria) {
			return new StatementExport("Reference\nTRFTEST".getBytes(StandardCharsets.UTF_8),
					"text/csv;charset=UTF-8", "statement.csv");
		}
		@Override public StatementExport exportPdf(Long userId, TransactionSearchCriteria criteria) {
			return new StatementExport("%PDF-1.4\n%%EOF".getBytes(StandardCharsets.ISO_8859_1),
					"application/pdf", "statement.pdf");
		}
		private TransactionView transaction() {
			return new TransactionView(40L, "********3333", "TRFTEST", TransactionDirection.DEBIT,
					AccountTransactionType.FUND_TRANSFER, new BigDecimal("100.00"), new BigDecimal("900.00"),
					"Receiver Customer", "********7777", "Invoice", LocalDateTime.of(2026, 8, 2, 10, 30));
		}
	}
}
