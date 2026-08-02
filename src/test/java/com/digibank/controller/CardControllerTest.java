package com.digibank.controller;

import com.digibank.dto.card.CardAccountOption;
import com.digibank.dto.card.CardRequest;
import com.digibank.dto.card.CardRequestFormView;
import com.digibank.dto.card.CardView;
import com.digibank.entity.User;
import com.digibank.enums.CardStatus;
import com.digibank.enums.CardType;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.Role;
import com.digibank.repository.UserRepository;
import com.digibank.security.CustomAuthenticationSuccessHandler;
import com.digibank.security.CustomUserDetails;
import com.digibank.security.SecurityConfig;
import com.digibank.service.CardManagementService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({CustomerCardController.class, StaffCardController.class})
@Import({SecurityConfig.class, CardControllerTest.CardTestConfig.class})
class CardControllerTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private FakeCardService cardService;
	private CustomUserDetails customer;
	private CustomUserDetails staff;

	@BeforeEach
	void setUp() {
		cardService.lastAction = null;
		customer = userDetails("customer", Role.CUSTOMER, 7L);
		staff = userDetails("staff", Role.BANK_STAFF, 8L);
	}

	@Test
	void customerCardPagesRenderAndStaffCannotAccessThem() throws Exception {
		mockMvc.perform(get("/customer/cards").with(user(customer)))
				.andExpect(status().isOk()).andExpect(view().name("customer/cards/list"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("My cards")));
		mockMvc.perform(get("/customer/cards/new").with(user(customer)))
				.andExpect(status().isOk()).andExpect(view().name("customer/cards/request"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Find your perfect card")));
		mockMvc.perform(get("/customer/cards").with(user(staff))).andExpect(status().isForbidden());
	}

	@Test
	void customerCanOpenAndActivateOwnedCardWithCsrf() throws Exception {
		mockMvc.perform(get("/customer/cards/CRDTEST").with(user(customer)))
				.andExpect(status().isOk()).andExpect(view().name("customer/cards/details"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Activate card")));
		mockMvc.perform(post("/customer/cards/CRDTEST/activate").with(user(customer))
				.param("transactionPin", "1234")).andExpect(status().isForbidden());
		mockMvc.perform(post("/customer/cards/CRDTEST/activate").with(user(customer)).with(csrf())
				.param("transactionPin", "1234")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/customer/cards/CRDTEST"));
		assertEquals("activate:CRDTEST", cardService.lastAction);
	}

	@Test
	void customerCanRevealIssuedCardNumberOnlyWithCsrf() throws Exception {
		mockMvc.perform(post("/customer/cards/CRDTEST/number").with(user(customer)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/customer/cards/CRDTEST/number").with(user(customer)).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.cardNumber").value("4532123412341234"));
		mockMvc.perform(post("/customer/cards/CRDTEST/number").with(user(staff)).with(csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void staffCanReviewAndApproveCardWithCsrf() throws Exception {
		mockMvc.perform(get("/staff/cards").with(user(staff)))
				.andExpect(status().isOk()).andExpect(view().name("staff/cards/list"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Card requests")));
		mockMvc.perform(get("/staff/cards/CRDTEST").with(user(staff)))
				.andExpect(status().isOk()).andExpect(view().name("staff/cards/details"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Approve and issue")));
		mockMvc.perform(post("/staff/cards/CRDTEST/approve").with(user(staff))).andExpect(status().isForbidden());
		mockMvc.perform(post("/staff/cards/CRDTEST/approve").with(user(staff)).with(csrf()))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/staff/cards/CRDTEST"));
		assertEquals("approve:CRDTEST", cardService.lastAction);
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
	static class CardTestConfig {
		@Bean FakeCardService cardManagementService() { return new FakeCardService(); }
		@Bean CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
			InvocationHandler handler = (proxy, method, args) -> method.getReturnType() == int.class ? 0 : null;
			UserRepository repository = UserRepository.class.cast(Proxy.newProxyInstance(
					UserRepository.class.getClassLoader(), new Class<?>[]{UserRepository.class}, handler));
			return new CustomAuthenticationSuccessHandler(repository);
		}
	}

	static class FakeCardService implements CardManagementService {
		private String lastAction;
		@Override public CardRequestFormView getRequestForm(Long userId) {
			return new CardRequestFormView(List.of(new CardAccountOption("111122223333", "SAVINGS")), CardType.values());
		}
		@Override public CardView requestCard(Long userId, String actor, CardRequest request) { return card(); }
		@Override public List<CardView> getCustomerCards(Long userId) { return List.of(card(CardStatus.INACTIVE)); }
		@Override public CardView getCustomerCard(Long userId, String number) { return card(CardStatus.INACTIVE); }
		@Override public String revealCardNumber(Long userId, String actor, String number) { return "4532123412341234"; }
		@Override public void activate(Long userId, String actor, String number, String pin) { lastAction = "activate:" + number; }
		@Override public void blockByCustomer(Long userId, String actor, String number, String pin) { lastAction = "block:" + number; }
		@Override public void reactivateByCustomer(Long userId, String actor, String number, String pin) { lastAction = "reactivate:" + number; }
		@Override public List<CardView> getCardsForReview(CardStatus status) { return List.of(card(status)); }
		@Override public CardView getCardForStaff(String number) { return card(CardStatus.PENDING_REVIEW); }
		@Override public void approve(String actor, String number, String note) { lastAction = "approve:" + number; }
		@Override public void reject(String actor, String number, String reason) { lastAction = "reject:" + number; }
		@Override public void blockByStaff(String actor, String number, String reason) { lastAction = "staff-block:" + number; }
		@Override public void reactivateByStaff(String actor, String number, String note) { lastAction = "staff-reactivate:" + number; }
		private CardView card() { return card(CardStatus.PENDING_REVIEW); }
		private CardView card(CardStatus status) {
			return new CardView("CRDTEST", "Test Customer", "CUS100", "********3333", CardType.DEBIT,
					status, "TEST CUSTOMER", status == CardStatus.PENDING_REVIEW ? "Not issued" : "•••• •••• •••• 1234",
					status == CardStatus.PENDING_REVIEW ? null : LocalDate.of(2031, 8, 31),
					LocalDateTime.of(2026, 8, 2, 10, 0), "staff", LocalDateTime.of(2026, 8, 2, 11, 0),
					"Approved", null, null, null);
		}
	}
}
