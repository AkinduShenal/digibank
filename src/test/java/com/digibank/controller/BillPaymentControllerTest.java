package com.digibank.controller;

import com.digibank.dto.bill.*;
import com.digibank.entity.User;
import com.digibank.enums.*;
import com.digibank.repository.UserRepository;
import com.digibank.security.*;
import com.digibank.service.BillPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CustomerBillPaymentController.class, StaffBillPaymentController.class})
@Import({SecurityConfig.class, BillPaymentControllerTest.Config.class})
class BillPaymentControllerTest {
	@Autowired MockMvc mvc;
	private CustomUserDetails customer;
	private CustomUserDetails staff;

	@BeforeEach void setup() {
		customer = userDetails("customer", Role.CUSTOMER, 10L);
		staff = userDetails("staff", Role.BANK_STAFF, 11L);
	}

	@Test void customerPagesRenderAndStaffIsDenied() throws Exception {
		mvc.perform(get("/customer/bill-payments").with(user(customer))).andExpect(status().isOk())
				.andExpect(view().name("customer/bills/history")).andExpect(content().string(org.hamcrest.Matchers.containsString("Bill payments")));
		mvc.perform(get("/customer/bill-payments/new").with(user(customer))).andExpect(status().isOk())
				.andExpect(view().name("customer/bills/new")).andExpect(content().string(org.hamcrest.Matchers.containsString("Pay a bill")));
		mvc.perform(get("/customer/bill-payments").with(user(staff))).andExpect(status().isForbidden());
	}

	@Test void paymentSubmissionRequiresCsrf() throws Exception {
		var request = post("/customer/bill-payments").with(user(customer)).param("sourceAccountNumber", "111122223333")
				.param("selectionType", "NEW_BILLER").param("provider", "CEB").param("consumerReference", "ACC12345")
				.param("amount", "100.00").param("transactionPin", "1234");
		mvc.perform(request).andExpect(status().isForbidden());
		mvc.perform(post("/customer/bill-payments").with(user(customer)).with(csrf())
				.param("sourceAccountNumber", "111122223333").param("selectionType", "NEW_BILLER")
				.param("provider", "CEB").param("consumerReference", "ACC12345")
				.param("amount", "100.00").param("transactionPin", "1234"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/customer/bill-payments/BILTEST"));
	}

	@Test void staffCanMonitorButCustomerCannotUseStaffPage() throws Exception {
		mvc.perform(get("/staff/bill-payments").with(user(staff))).andExpect(status().isOk())
				.andExpect(view().name("staff/bills/list")).andExpect(content().string(org.hamcrest.Matchers.containsString("Payment operations")));
		mvc.perform(get("/staff/bill-payments/BILTEST").with(user(staff))).andExpect(status().isOk())
				.andExpect(view().name("staff/bills/details"));
		mvc.perform(get("/staff/bill-payments").with(user(customer))).andExpect(status().isForbidden());
	}

	private CustomUserDetails userDetails(String name, Role role, Long id) {
		User user = new User(name, name + "@test.local", "hash"); user.setRole(role); user.setEnabled(true);
		try { Method m = User.class.getSuperclass().getDeclaredMethod("setId", Long.class); m.setAccessible(true); m.invoke(user, id); }
		catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
		return new CustomUserDetails(user, CustomerStatus.ACTIVE);
	}

	@TestConfiguration static class Config {
		@Bean BillPaymentService billPaymentService() { return new FakeService(); }
		@Bean CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
			InvocationHandler handler = (proxy, method, args) -> method.getReturnType() == int.class ? 0 : null;
			UserRepository repo = UserRepository.class.cast(Proxy.newProxyInstance(UserRepository.class.getClassLoader(), new Class<?>[]{UserRepository.class}, handler));
			return new CustomAuthenticationSuccessHandler(repo);
		}
	}

	static class FakeService implements BillPaymentService {
		private final LocalDateTime time = LocalDateTime.of(2026, 8, 2, 20, 0);
		@Override public BillPaymentFormView getPaymentForm(Long id) { return new BillPaymentFormView(
				List.of(new BillAccountOption("111122223333", "********3333", "SAVINGS", new BigDecimal("1000.00"))),
				List.of(), List.of(new BillerProviderOption(BillerProvider.CEB, BillerCategory.ELECTRICITY,
				"Ceylon Electricity Board", "Electricity account number")), BillerCategory.values()); }
		@Override public BillPaymentDetailsView pay(Long id, String actor, BillPaymentRequest r) { return detail(); }
		@Override public List<BillPaymentListView> getCustomerHistory(Long id) { return List.of(summary()); }
		@Override public BillPaymentDetailsView getCustomerPayment(Long id, String ref) { return detail(); }
		@Override public List<SavedBillerView> getSavedBillers(Long id) { return List.of(); }
		@Override public SavedBillerView saveBiller(Long id, String actor, SavedBillerRequest r) { return null; }
		@Override public void deleteBiller(Long id, String actor, Long biller) { }
		@Override public List<BillPaymentListView> getStaffPayments() { return List.of(summary()); }
		@Override public BillPaymentDetailsView getStaffPayment(String ref) { return detail(); }
		private BillPaymentListView summary() { return new BillPaymentListView("BILTEST", "Test Customer",
				"Ceylon Electricity Board", BillerCategory.ELECTRICITY, "*****2345", new BigDecimal("100.00"), BillPaymentStatus.COMPLETED, time); }
		private BillPaymentDetailsView detail() { return new BillPaymentDetailsView("BILTEST", "Test Customer", "CUS100",
				"********3333", "Ceylon Electricity Board", BillerCategory.ELECTRICITY, "*****2345",
				new BigDecimal("100.00"), BillPaymentStatus.COMPLETED, new BigDecimal("900.00"), "August", time); }
	}
}
