package com.digibank.controller;

import com.digibank.dto.staff.StaffCustomerView;
import com.digibank.dto.staff.StaffDashboardView;
import com.digibank.enums.AccountStatus;
import com.digibank.enums.AccountType;
import com.digibank.enums.BeneficiaryVerificationStatus;
import com.digibank.enums.CustomerStatus;
import com.digibank.exception.AccountAccessDeniedException;
import com.digibank.exception.BeneficiaryNotFoundException;
import com.digibank.exception.CustomerProfileNotFoundException;
import com.digibank.exception.InvalidBeneficiaryException;
import com.digibank.exception.InvalidBeneficiaryStateException;
import com.digibank.exception.InvalidAccountClosureException;
import com.digibank.exception.InvalidAccountStateTransitionException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.AccountManagementService;
import com.digibank.service.BeneficiaryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.digibank.util.PageSupport;

@Controller
public class StaffAccountManagementController {

	private final AccountManagementService accountManagementService;
	private final BeneficiaryService beneficiaryService;

	public StaffAccountManagementController(AccountManagementService accountManagementService,
			BeneficiaryService beneficiaryService) {
		this.accountManagementService = accountManagementService;
		this.beneficiaryService = beneficiaryService;
	}

	@GetMapping("/staff/dashboard")
	public String staffDashboard(Model model) {
		List<StaffCustomerView> customers = accountManagementService.getCustomerRecords();
		long pendingBeneficiaries = beneficiaryService
				.getBeneficiariesForReview(BeneficiaryVerificationStatus.PENDING).size();
		model.addAttribute("dashboard", dashboard(customers, pendingBeneficiaries));
		model.addAttribute("recentCustomers", customers.stream()
				.sorted(Comparator.comparing(StaffCustomerView::getJoinedAt,
						Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(5)
				.toList());
		return "staff/dashboard";
	}

	@GetMapping("/staff/customers")
	public String customers(@RequestParam(required = false) String q,
			@RequestParam(required = false) CustomerStatus status,@RequestParam(defaultValue="0") int page,
			@RequestParam(defaultValue="20") int size, Model model) {
		String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
		List<StaffCustomerView> customers = accountManagementService.getCustomerRecords().stream()
				.filter(customer -> status == null || customer.getCustomerStatus() == status)
				.filter(customer -> query.isEmpty()
						|| customer.getCustomerNumber().toLowerCase(Locale.ROOT).contains(query)
						|| customer.getFullName().toLowerCase(Locale.ROOT).contains(query)
						|| customer.getEmail().toLowerCase(Locale.ROOT).contains(query))
				.toList();
		var result=PageSupport.page(customers,page,size);model.addAttribute("customers", result.getContent());model.addAttribute("customersPage",result);
		model.addAttribute("query", q == null ? "" : q.trim());
		model.addAttribute("selectedStatus", status);
		model.addAttribute("customerStatuses", CustomerStatus.values());
		return "staff/customers";
	}

	@GetMapping("/staff/customers/{customerNumber}")
	public String customerDetails(@PathVariable String customerNumber, Model model) {
		model.addAttribute("customer", accountManagementService.getCustomerDetails(customerNumber));
		model.addAttribute("accountTypes", AccountType.values());
		return "staff/customer-details";
	}

	@GetMapping("/staff/beneficiaries")
	public String beneficiaryReviews(
			@RequestParam(defaultValue = "PENDING") BeneficiaryVerificationStatus status, Model model) {
		model.addAttribute("beneficiaries", beneficiaryService.getBeneficiariesForReview(status));
		model.addAttribute("selectedStatus", status);
		model.addAttribute("verificationStatuses", BeneficiaryVerificationStatus.values());
		return "staff/beneficiaries";
	}

	@PostMapping("/staff/beneficiaries/{beneficiaryId}/verify")
	public String verifyBeneficiary(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long beneficiaryId, @RequestParam(required = false) String note,
			RedirectAttributes redirectAttributes) {
		try {
			beneficiaryService.verifyBeneficiary(userDetails.getUsername(), beneficiaryId, note);
			redirectAttributes.addFlashAttribute("successMessage", "Beneficiary verified successfully.");
		}
		catch (InvalidBeneficiaryException | InvalidBeneficiaryStateException | BeneficiaryNotFoundException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/staff/beneficiaries";
	}

	@PostMapping("/staff/beneficiaries/{beneficiaryId}/reject")
	public String rejectBeneficiary(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long beneficiaryId, @RequestParam String reason,
			RedirectAttributes redirectAttributes) {
		try {
			beneficiaryService.rejectBeneficiary(userDetails.getUsername(), beneficiaryId, reason);
			redirectAttributes.addFlashAttribute("successMessage", "Beneficiary verification rejected.");
		}
		catch (InvalidBeneficiaryException | InvalidBeneficiaryStateException | BeneficiaryNotFoundException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/staff/beneficiaries";
	}

	@PostMapping("/staff/accounts/{accountNumber}/activate")
	public String activate(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String accountNumber,
			@RequestParam String customerNumber, RedirectAttributes redirectAttributes) {
		accountManagementService.activateAccount(userDetails.getUsername(), accountNumber);
		return success(customerNumber, "Account activated.", redirectAttributes);
	}

	@PostMapping("/staff/accounts/{accountNumber}/freeze")
	public String freeze(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String accountNumber,
			@RequestParam String customerNumber, @RequestParam String reason, RedirectAttributes redirectAttributes) {
		accountManagementService.freezeAccount(userDetails.getUsername(), accountNumber, reason);
		return success(customerNumber, "Account frozen.", redirectAttributes);
	}

	@PostMapping("/staff/accounts/{accountNumber}/unfreeze")
	public String unfreeze(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String accountNumber,
			@RequestParam String customerNumber, @RequestParam String reason, RedirectAttributes redirectAttributes) {
		accountManagementService.unfreezeAccount(userDetails.getUsername(), accountNumber, reason);
		return success(customerNumber, "Account unfrozen.", redirectAttributes);
	}

	@PostMapping("/staff/accounts/{accountNumber}/deactivate")
	public String deactivate(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String accountNumber,
			@RequestParam String customerNumber, @RequestParam String reason, RedirectAttributes redirectAttributes) {
		accountManagementService.deactivateAccount(userDetails.getUsername(), accountNumber, reason);
		return success(customerNumber, "Customer access and account were deactivated.", redirectAttributes);
	}

	@PostMapping("/staff/accounts/{accountNumber}/reactivate")
	public String reactivate(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String accountNumber,
			@RequestParam String customerNumber, @RequestParam String reason, RedirectAttributes redirectAttributes) {
		accountManagementService.reactivateEligibleAccount(userDetails.getUsername(), accountNumber, reason);
		return success(customerNumber, "Eligible account reactivated.", redirectAttributes);
	}

	@PostMapping("/staff/accounts/{accountNumber}/close")
	public String close(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String accountNumber,
			@RequestParam String customerNumber, @RequestParam String reason, RedirectAttributes redirectAttributes) {
		accountManagementService.closeAccount(userDetails.getUsername(), customerNumber, accountNumber, reason);
		return success(customerNumber, "Account closed.", redirectAttributes);
	}

	@PostMapping("/staff/accounts/{accountNumber}/type")
	public String changeType(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String accountNumber,
			@RequestParam String customerNumber, @RequestParam AccountType accountType, @RequestParam String reason,
			RedirectAttributes redirectAttributes) {
		accountManagementService.changeAccountType(userDetails.getUsername(), accountNumber, accountType, reason);
		return success(customerNumber, "Account type updated.", redirectAttributes);
	}

	@ExceptionHandler({
			AccountAccessDeniedException.class,
			CustomerProfileNotFoundException.class,
			InvalidAccountClosureException.class,
			InvalidAccountStateTransitionException.class
	})
	public String handleManagementError(RuntimeException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		return "redirect:/staff/customers";
	}

	private String success(String customerNumber, String message, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("successMessage", message);
		return "redirect:/staff/customers/" + customerNumber;
	}

	private StaffDashboardView dashboard(List<StaffCustomerView> customers, long pendingBeneficiaries) {
		long totalAccounts = customers.stream().mapToLong(customer -> customer.getAccounts().size()).sum();
		long pendingAccounts = customers.stream().flatMap(customer -> customer.getAccounts().stream())
				.filter(account -> account.getAccountStatus() == AccountStatus.PENDING_ACTIVATION).count();
		long frozenAccounts = customers.stream().flatMap(customer -> customer.getAccounts().stream())
				.filter(account -> account.getAccountStatus() == AccountStatus.FROZEN).count();
		long pendingCustomers = customers.stream()
				.filter(customer -> customer.getCustomerStatus() == CustomerStatus.PENDING_VERIFICATION).count();
		long activeCustomers = customers.stream()
				.filter(customer -> customer.getCustomerStatus() == CustomerStatus.ACTIVE).count();
		return new StaffDashboardView(customers.size(), pendingCustomers, activeCustomers, totalAccounts,
				pendingAccounts, frozenAccounts, pendingBeneficiaries);
	}
}
