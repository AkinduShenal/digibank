package com.digibank.controller;

import com.digibank.exception.BillPaymentException;
import com.digibank.service.BillPaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;
import com.digibank.enums.BillerCategory;
import com.digibank.util.PageSupport;
import java.util.Locale;

@Controller
public class StaffBillPaymentController {
	private final BillPaymentService service;

	public StaffBillPaymentController(BillPaymentService service) { this.service = service; }

	@GetMapping("/staff/bill-payments")
	public String payments(@RequestParam(required=false) String q,@RequestParam(required=false) BillerCategory category,
			@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,Model model) {
		String query=PageSupport.query(q);var filtered=service.getStaffPayments().stream().filter(p->category==null||p.category()==category)
				.filter(p->query.isEmpty()||p.referenceNumber().toLowerCase(Locale.ROOT).contains(query)||p.customerName().toLowerCase(Locale.ROOT).contains(query)||p.providerName().toLowerCase(Locale.ROOT).contains(query)).toList();var result=PageSupport.page(filtered,page,size);
		model.addAttribute("payments", result.getContent());model.addAttribute("paymentsPage",result);model.addAttribute("query",q==null?"":q.trim());model.addAttribute("selectedCategory",category);model.addAttribute("billerCategories",BillerCategory.values());
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
