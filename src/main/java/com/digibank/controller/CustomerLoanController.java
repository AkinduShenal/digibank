package com.digibank.controller;

import com.digibank.dto.loan.LoanApplicationRequest;
import com.digibank.dto.loan.LoanRepaymentRequest;
import com.digibank.exception.LoanException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.LoanManagementService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerLoanController {

	private final LoanManagementService loanService;

	public CustomerLoanController(LoanManagementService loanService) {
		this.loanService = loanService;
	}

	@GetMapping("/customer/loans")
	public String loans(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		model.addAttribute("loans", loanService.getCustomerLoans(userDetails.getUserId()));
		return "customer/loans/list";
	}

	@GetMapping("/customer/loans/new")
	public String newLoan(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		if (!model.containsAttribute("loanApplicationRequest")) {
			model.addAttribute("loanApplicationRequest", new LoanApplicationRequest());
		}
		model.addAttribute("form", loanService.getApplicationForm(userDetails.getUserId()));
		return "customer/loans/apply";
	}

	@PostMapping("/customer/loans")
	public String apply(@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @ModelAttribute LoanApplicationRequest loanApplicationRequest, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("form", loanService.getApplicationForm(userDetails.getUserId()));
			return "customer/loans/apply";
		}
		try {
			var loan = loanService.apply(userDetails.getUserId(), userDetails.getUsername(), loanApplicationRequest);
			redirectAttributes.addFlashAttribute("successMessage", "Loan application submitted successfully.");
			return "redirect:/customer/loans/" + loan.applicationNumber();
		}
		catch (LoanException ex) {
			model.addAttribute("errorMessage", ex.getMessage());
			model.addAttribute("form", loanService.getApplicationForm(userDetails.getUserId()));
			return "customer/loans/apply";
		}
	}

	@GetMapping("/customer/loans/{applicationNumber}")
	public String details(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable String applicationNumber, Model model, RedirectAttributes redirectAttributes) {
		try {
			model.addAttribute("loan", loanService.getCustomerLoan(userDetails.getUserId(), applicationNumber));
			model.addAttribute("repaymentAccounts", loanService.getRepaymentAccounts(userDetails.getUserId()));
			return "customer/loans/details";
		}
		catch (LoanException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/customer/loans";
		}
	}

	@PostMapping("/customer/loans/{applicationNumber}/repayments/{installmentNumber}")
	public String repay(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable String applicationNumber, @PathVariable int installmentNumber,
			@Valid @ModelAttribute LoanRepaymentRequest loanRepaymentRequest, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			String message = bindingResult.getAllErrors().getFirst().getDefaultMessage();
			redirectAttributes.addFlashAttribute("errorMessage", message);
			return "redirect:/customer/loans/" + applicationNumber;
		}
		try {
			String reference = loanService.payInstallment(userDetails.getUserId(), userDetails.getUsername(),
					applicationNumber, installmentNumber, loanRepaymentRequest);
			redirectAttributes.addFlashAttribute("successMessage",
					"Installment paid successfully. Reference: " + reference);
		}
		catch (LoanException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/customer/loans/" + applicationNumber;
	}
}
