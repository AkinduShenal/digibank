package com.digibank.controller;

import com.digibank.dto.customer.CustomerProfileUpdateRequest;
import com.digibank.exception.AccountAccessDeniedException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.AccountManagementService;
import com.digibank.service.CustomerProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerController {

	private final CustomerProfileService customerProfileService;
	private final AccountManagementService accountManagementService;

	public CustomerController(CustomerProfileService customerProfileService, AccountManagementService accountManagementService) {
		this.customerProfileService = customerProfileService;
		this.accountManagementService = accountManagementService;
	}

	@GetMapping("/customer/dashboard")
	public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		model.addAttribute("dashboard", customerProfileService.getDashboard(userDetails));
		return "customer/dashboard";
	}

	@GetMapping("/customer/profile")
	public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		model.addAttribute("profile", customerProfileService.getProfile(userDetails));
		return "customer/profile";
	}

	@GetMapping("/customer/profile/edit")
	public String editProfile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		if (!model.containsAttribute("profileUpdateRequest")) {
			model.addAttribute("profileUpdateRequest", customerProfileService.getProfileUpdateRequest(userDetails));
		}
		model.addAttribute("profile", customerProfileService.getProfile(userDetails));
		return "customer/profile-edit";
	}

	@PostMapping("/customer/profile/edit")
	public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @ModelAttribute("profileUpdateRequest") CustomerProfileUpdateRequest request,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("profile", customerProfileService.getProfile(userDetails));
			return "customer/profile-edit";
		}
		customerProfileService.updateProfile(userDetails, request);
		redirectAttributes.addFlashAttribute("successMessage", "Your profile was updated successfully.");
		return "redirect:/customer/profile";
	}

	@GetMapping("/customer/accounts/{accountNumber}")
	public String accountDetails(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable String accountNumber, Model model) {
		model.addAttribute("account", customerProfileService.getAccountDetails(userDetails, accountNumber));
		return "account/details";
	}

	@PostMapping("/customer/accounts/{accountNumber}/closure-request")
	public String requestAccountClosure(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable String accountNumber, String reason, RedirectAttributes redirectAttributes) {
		customerProfileService.getAccountDetails(userDetails, accountNumber);
		accountManagementService.requestAccountClosure(userDetails.getUsername(), accountNumber, reason);
		redirectAttributes.addFlashAttribute("successMessage", "Your account closure request was sent for staff review.");
		return "redirect:/customer/accounts/" + accountNumber;
	}

	@ExceptionHandler(AccountAccessDeniedException.class)
	public String handleAccountAccessDenied(Model model) {
		model.addAttribute("errorMessage", "That account is not available for your DigiBank profile.");
		return "customer/access-denied";
	}
}
