package com.digibank.controller;

import com.digibank.dto.loan.LoanAccountOption;
import com.digibank.dto.loan.LoanApplicationFormView;
import com.digibank.dto.loan.LoanApplicationRequest;
import com.digibank.dto.loan.LoanApplicationView;
import com.digibank.dto.loan.LoanScheduleView;
import com.digibank.dto.loan.LoanRepaymentRequest;
import com.digibank.entity.User;
import com.digibank.enums.CustomerStatus;
import com.digibank.enums.LoanStatus;
import com.digibank.enums.LoanType;
import com.digibank.enums.RepaymentStatus;
import com.digibank.enums.Role;
import com.digibank.repository.UserRepository;
import com.digibank.security.CustomAuthenticationSuccessHandler;
import com.digibank.security.CustomUserDetails;
import com.digibank.security.SecurityConfig;
import com.digibank.service.LoanManagementService;
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
import java.math.BigDecimal;
import java.time.LocalDate;
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

@WebMvcTest({CustomerLoanController.class, StaffLoanController.class})
@Import({SecurityConfig.class, LoanControllerTest.LoanTestConfig.class})
class LoanControllerTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private FakeLoanService loanService;
	private CustomUserDetails customer;
	private CustomUserDetails staff;

	@BeforeEach
	void setUp() {
		loanService.approvedNumber = null;
		loanService.paidNumber = null;
		customer = userDetails("customer", Role.CUSTOMER, 7L);
		staff = userDetails("staff", Role.BANK_STAFF, 8L);
	}

	@Test
	void customerRepaymentRequiresCsrfAndValidPinShape() throws Exception {
		mockMvc.perform(post("/customer/loans/LONTEST/repayments/1").with(user(customer))
				.param("accountNumber", "111122223333").param("transactionPin", "1234"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/customer/loans/LONTEST/repayments/1").with(user(customer)).with(csrf())
				.param("accountNumber", "111122223333").param("transactionPin", "1234"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/customer/loans/LONTEST"));
		assertEquals("LONTEST", loanService.paidNumber);
	}

	@Test
	void customerLoanPagesRenderAndStaffCannotAccessThem() throws Exception {
		mockMvc.perform(get("/customer/loans").with(user(customer)))
				.andExpect(status().isOk()).andExpect(view().name("customer/loans/list"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("My loans")));
		mockMvc.perform(get("/customer/loans/new").with(user(customer)))
				.andExpect(status().isOk()).andExpect(view().name("customer/loans/apply"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Apply for a loan")));
		mockMvc.perform(get("/customer/loans").with(user(staff))).andExpect(status().isForbidden());
	}

	@Test
	void customerCanSeeOwnedLoanAndRepaymentSchedule() throws Exception {
		mockMvc.perform(get("/customer/loans/LONTEST").with(user(customer)))
				.andExpect(status().isOk()).andExpect(view().name("customer/loans/details"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Installment schedule")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("LONTEST")));
	}

	@Test
	void staffCanReviewAndApproveWithCsrf() throws Exception {
		mockMvc.perform(get("/staff/loans").with(user(staff)))
				.andExpect(status().isOk()).andExpect(view().name("staff/loans/list"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Loan applications")));
		mockMvc.perform(get("/staff/loans/LONTEST").with(user(staff)))
				.andExpect(status().isOk()).andExpect(view().name("staff/loans/details"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Approve and disburse")));
		mockMvc.perform(post("/staff/loans/LONTEST/approve").with(user(staff))
				.param("approvedAmount", "100000.00")).andExpect(status().isForbidden());
		mockMvc.perform(post("/staff/loans/LONTEST/approve").with(user(staff)).with(csrf())
				.param("approvedAmount", "100000.00")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/staff/loans/LONTEST"));
		assertEquals("LONTEST", loanService.approvedNumber);
	}

	@Test
	void loanDocumentsAreDownloadedOnlyThroughTheCorrectRoleRoute() throws Exception {
		mockMvc.perform(get("/customer/loans/LONTEST/document").with(user(customer)))
				.andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
		mockMvc.perform(get("/customer/loans/LONTEST/document").with(user(staff))).andExpect(status().isForbidden());
		mockMvc.perform(get("/staff/loans/LONTEST/document").with(user(staff)))
				.andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
		mockMvc.perform(get("/staff/loans/LONTEST/document").with(user(customer))).andExpect(status().isForbidden());
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
	static class LoanTestConfig {
		@Bean FakeLoanService loanManagementService() { return new FakeLoanService(); }
		@Bean CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
			InvocationHandler handler = (proxy, method, args) -> method.getReturnType() == int.class ? 0 : null;
			UserRepository repository = UserRepository.class.cast(Proxy.newProxyInstance(
					UserRepository.class.getClassLoader(), new Class<?>[]{UserRepository.class}, handler));
			return new CustomAuthenticationSuccessHandler(repository);
		}
	}

	static class FakeLoanService implements LoanManagementService {
		private String approvedNumber;
		private String paidNumber;
		@Override public LoanApplicationFormView getApplicationForm(Long userId) {
			return new LoanApplicationFormView(List.of(new LoanAccountOption("111122223333", "SAVINGS",
					new BigDecimal("1000.00"))), LoanType.values());
		}
		@Override public LoanApplicationView apply(Long userId, String actor, LoanApplicationRequest request) { return loan(); }
		@Override public LoanApplicationRequest getPendingApplicationForEdit(Long userId,String number){return new LoanApplicationRequest();}
		@Override public void updatePendingApplication(Long userId,String actor,String number,LoanApplicationRequest request){}
		@Override public void withdrawPendingApplication(Long userId,String actor,String number){}
		@Override public List<LoanApplicationView> getCustomerLoans(Long userId) { return List.of(loan()); }
		@Override public LoanApplicationView getCustomerLoan(Long userId, String number) { return loan(); }
		@Override public List<LoanApplicationView> getLoansForReview(LoanStatus status) { return List.of(loan()); }
		@Override public LoanApplicationView getLoanForStaff(String number) { return loan(); }
		@Override public com.digibank.dto.loan.LoanDocumentDownload getCustomerDocument(Long id,String number){return new com.digibank.dto.loan.LoanDocumentDownload("doc.pdf","application/pdf","%PDF-".getBytes());}
		@Override public com.digibank.dto.loan.LoanDocumentDownload getStaffDocument(String number){return new com.digibank.dto.loan.LoanDocumentDownload("doc.pdf","application/pdf","%PDF-".getBytes());}
		@Override public void approveAndDisburse(String actor, String number, BigDecimal amount, String note) { approvedNumber = number; }
		@Override public void reject(String actor, String number, String reason) { }
		@Override public List<LoanAccountOption> getRepaymentAccounts(Long userId) { return getApplicationForm(userId).accounts(); }
		@Override public String payInstallment(Long userId, String actor, String number, int installment,
				LoanRepaymentRequest request) { paidNumber = number; return "LRPTEST"; }
		private LoanApplicationView loan() {
			return new LoanApplicationView("LONTEST", "CUS100", "Test Customer", "********3333",
					LoanType.PERSONAL, LoanStatus.PENDING_REVIEW, new BigDecimal("100000.00"), null,
					new BigDecimal("12.00"), 12, new BigDecimal("8884.88"), new BigDecimal("50000.00"),
					"Permanent employee", "Personal home improvements", "PAYSLIP-TEST-001", false, null, null, null, null,
					LocalDateTime.of(2026, 8, 2, 10, 0), List.of(new LoanScheduleView(1,
					LocalDate.of(2026, 9, 2), new BigDecimal("7884.88"), new BigDecimal("1000.00"),
					new BigDecimal("8884.88"), RepaymentStatus.SCHEDULED, true, null, null)));
		}
	}
}
