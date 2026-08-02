package com.digibank.controller;

import com.digibank.enums.TransferStatus;
import com.digibank.exception.TransferException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.TransferReversalService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff/transfers")
public class StaffTransferController {
	private final TransferReversalService service;

	public StaffTransferController(TransferReversalService service) { this.service = service; }

	@GetMapping
	public String list(@RequestParam(required = false) String query,
			@RequestParam(required = false) TransferStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			Model model) {
		model.addAttribute("transfers", service.search(query, status, page, size));
		model.addAttribute("query", query);
		model.addAttribute("selectedStatus", status);
		model.addAttribute("transferStatuses", TransferStatus.values());
		return "staff/transfers/list";
	}

	@GetMapping("/{referenceNumber}")
	public String details(@PathVariable String referenceNumber, Model model, RedirectAttributes redirect) {
		try {
			model.addAttribute("transfer", service.get(referenceNumber));
			return "staff/transfers/details";
		} catch (TransferException ex) {
			redirect.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/staff/transfers";
		}
	}

	@PostMapping("/{referenceNumber}/reverse")
	public String reverse(@AuthenticationPrincipal CustomUserDetails user,
			@PathVariable String referenceNumber, @RequestParam String reason, RedirectAttributes redirect) {
		try {
			String reversal = service.reverse(user.getUsername(), referenceNumber, reason);
			redirect.addFlashAttribute("successMessage", "Transfer reversed successfully. Reversal reference: " + reversal);
		} catch (TransferException ex) {
			redirect.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/staff/transfers/" + referenceNumber;
	}
}
