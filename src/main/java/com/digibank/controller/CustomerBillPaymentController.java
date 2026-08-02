package com.digibank.controller;

import com.digibank.dto.bill.BillPaymentRequest;
import com.digibank.dto.bill.SavedBillerRequest;
import com.digibank.enums.BillerProvider;
import com.digibank.enums.BillerSelectionType;
import com.digibank.exception.BillPaymentException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.BillPaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer/bill-payments")
public class CustomerBillPaymentController {
	private final BillPaymentService service;

	public CustomerBillPaymentController(BillPaymentService service) { this.service = service; }

	@GetMapping
	public String history(@AuthenticationPrincipal CustomUserDetails user, Model model) {
		model.addAttribute("payments", service.getCustomerHistory(user.getUserId()));
		return "customer/bills/history";
	}

	@GetMapping("/new")
	public String form(@AuthenticationPrincipal CustomUserDetails user, Model model) {
		if (!model.containsAttribute("paymentRequest")) {
			BillPaymentRequest request = new BillPaymentRequest();
			request.setSelectionType(BillerSelectionType.NEW_BILLER);
			model.addAttribute("paymentRequest", request);
		}
		model.addAttribute("form", service.getPaymentForm(user.getUserId()));
		model.addAttribute("selectionTypes", BillerSelectionType.values());
		return "customer/bills/new";
	}

	@PostMapping
	public String pay(@AuthenticationPrincipal CustomUserDetails user,
			@Valid @ModelAttribute("paymentRequest") BillPaymentRequest request, BindingResult bindingResult,
			Model model, RedirectAttributes redirect) {
		if (bindingResult.hasErrors()) return paymentForm(user, model);
		try {
			var receipt = service.pay(user.getUserId(), user.getUsername(), request);
			redirect.addFlashAttribute("successMessage", "Bill payment completed successfully.");
			return "redirect:/customer/bill-payments/" + receipt.referenceNumber();
		} catch (BillPaymentException ex) {
			model.addAttribute("errorMessage", ex.getMessage());
			return paymentForm(user, model);
		}
	}

	@GetMapping("/billers")
	public String billers(@AuthenticationPrincipal CustomUserDetails user, Model model) {
		if (!model.containsAttribute("savedBillerRequest")) model.addAttribute("savedBillerRequest", new SavedBillerRequest());
		model.addAttribute("billers", service.getSavedBillers(user.getUserId()));
		model.addAttribute("providers", BillerProvider.values());
		return "customer/bills/billers";
	}

	@PostMapping("/billers")
	public String saveBiller(@AuthenticationPrincipal CustomUserDetails user,
			@Valid @ModelAttribute SavedBillerRequest savedBillerRequest, BindingResult result,
			Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			model.addAttribute("billers", service.getSavedBillers(user.getUserId()));
			model.addAttribute("providers", BillerProvider.values());
			return "customer/bills/billers";
		}
		try {
			service.saveBiller(user.getUserId(), user.getUsername(), savedBillerRequest);
			redirect.addFlashAttribute("successMessage", "Biller saved successfully.");
		} catch (BillPaymentException ex) { redirect.addFlashAttribute("errorMessage", ex.getMessage()); }
		return "redirect:/customer/bill-payments/billers";
	}

	@PostMapping("/billers/{id}/delete")
	public String deleteBiller(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
			RedirectAttributes redirect) {
		try {
			service.deleteBiller(user.getUserId(), user.getUsername(), id);
			redirect.addFlashAttribute("successMessage", "Saved biller removed.");
		} catch (BillPaymentException ex) { redirect.addFlashAttribute("errorMessage", ex.getMessage()); }
		return "redirect:/customer/bill-payments/billers";
	}

	@GetMapping("/{referenceNumber}")
	public String receipt(@AuthenticationPrincipal CustomUserDetails user, @PathVariable String referenceNumber,
			Model model, RedirectAttributes redirect) {
		try {
			model.addAttribute("payment", service.getCustomerPayment(user.getUserId(), referenceNumber));
			return "customer/bills/details";
		} catch (BillPaymentException ex) {
			redirect.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/customer/bill-payments";
		}
	}

	private String paymentForm(CustomUserDetails user, Model model) {
		model.addAttribute("form", service.getPaymentForm(user.getUserId()));
		model.addAttribute("selectionTypes", BillerSelectionType.values());
		return "customer/bills/new";
	}
}
