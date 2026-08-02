package com.digibank.controller;

import com.digibank.exception.BillPaymentException;
import com.digibank.service.BillPaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StaffBillPaymentController {
	private final BillPaymentService service;

	public StaffBillPaymentController(BillPaymentService service) { this.service = service; }

	@GetMapping("/staff/bill-payments")
	public String payments(Model model) {
		model.addAttribute("payments", service.getStaffPayments());
		return "staff/bills/list";
	}

	@GetMapping("/staff/bill-payments/{referenceNumber}")
	public String details(@PathVariable String referenceNumber, Model model, RedirectAttributes redirect) {
		try {
			model.addAttribute("payment", service.getStaffPayment(referenceNumber));
			return "staff/bills/details";
		} catch (BillPaymentException ex) {
			redirect.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/staff/bill-payments";
		}
	}
}
