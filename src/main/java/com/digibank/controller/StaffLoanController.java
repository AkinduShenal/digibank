package com.digibank.controller;

import com.digibank.enums.LoanStatus;
import com.digibank.exception.LoanException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.LoanManagementService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class StaffLoanController {

	private final LoanManagementService loanService;

	public StaffLoanController(LoanManagementService loanService) {
		this.loanService = loanService;
	}

	@GetMapping("/staff/loans")
	public String loans(@RequestParam(defaultValue = "PENDING_REVIEW") LoanStatus status, Model model) {
		model.addAttribute("loans", loanService.getLoansForReview(status));
		model.addAttribute("selectedStatus", status);
		model.addAttribute("loanStatuses", LoanStatus.values());
		return "staff/loans/list";
	}

	@GetMapping("/staff/loans/{applicationNumber}")
	public String details(@PathVariable String applicationNumber, Model model, RedirectAttributes redirectAttributes) {
		try {
			model.addAttribute("loan", loanService.getLoanForStaff(applicationNumber));
			return "staff/loans/details";
		}
		catch (LoanException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/staff/loans";
		}
	}

	@PostMapping("/staff/loans/{applicationNumber}/approve")
	public String approve(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable String applicationNumber, @RequestParam BigDecimal approvedAmount,
			@RequestParam(required = false) String note, RedirectAttributes redirectAttributes) {
		try {
			loanService.approveAndDisburse(userDetails.getUsername(), applicationNumber, approvedAmount, note);
			redirectAttributes.addFlashAttribute("successMessage", "Loan approved and funds credited successfully.");
		}
		catch (LoanException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/staff/loans/" + applicationNumber;
	}

	@PostMapping("/staff/loans/{applicationNumber}/reject")
	public String reject(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable String applicationNumber, @RequestParam String reason,
			RedirectAttributes redirectAttributes) {
		try {
			loanService.reject(userDetails.getUsername(), applicationNumber, reason);
			redirectAttributes.addFlashAttribute("successMessage", "Loan application rejected.");
		}
		catch (LoanException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/staff/loans/" + applicationNumber;
	}
}
