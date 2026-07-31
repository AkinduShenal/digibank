package com.digibank.controller;

import com.digibank.enums.AccountType;
import com.digibank.exception.AccountAccessDeniedException;
import com.digibank.exception.CustomerProfileNotFoundException;
import com.digibank.exception.InvalidAccountClosureException;
import com.digibank.exception.InvalidAccountStateTransitionException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.AccountManagementService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StaffAccountManagementController {

	private final AccountManagementService accountManagementService;

	public StaffAccountManagementController(AccountManagementService accountManagementService) {
		this.accountManagementService = accountManagementService;
	}

	@GetMapping("/staff/dashboard")
	public String staffDashboard() {
		return "redirect:/staff/customers";
	}

	@GetMapping("/admin/dashboard")
	public String adminDashboard() {
		return "redirect:/staff/customers";
	}

	@GetMapping("/staff/customers")
	public String customers(Model model) {
		model.addAttribute("customers", accountManagementService.getCustomerRecords());
		return "staff/customers";
	}

	@GetMapping("/staff/customers/{customerNumber}")
	public String customerDetails(@PathVariable String customerNumber, Model model) {
		model.addAttribute("customer", accountManagementService.getCustomerDetails(customerNumber));
		model.addAttribute("accountTypes", AccountType.values());
		return "staff/customer-details";
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
}
