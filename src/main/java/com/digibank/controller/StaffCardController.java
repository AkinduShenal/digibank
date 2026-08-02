package com.digibank.controller;

import com.digibank.enums.CardStatus;
import com.digibank.exception.CardException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.CardManagementService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StaffCardController {

	private final CardManagementService cardService;

	public StaffCardController(CardManagementService cardService) {
		this.cardService = cardService;
	}

	@GetMapping("/staff/cards")
	public String cards(@RequestParam(defaultValue = "PENDING_REVIEW") CardStatus status, Model model) {
		model.addAttribute("cards", cardService.getCardsForReview(status));
		model.addAttribute("selectedStatus", status);
		model.addAttribute("cardStatuses", CardStatus.values());
		return "staff/cards/list";
	}

	@GetMapping("/staff/cards/{requestNumber}")
	public String details(@PathVariable String requestNumber, Model model, RedirectAttributes redirectAttributes) {
		try {
			model.addAttribute("card", cardService.getCardForStaff(requestNumber));
			return "staff/cards/details";
		}
		catch (CardException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/staff/cards";
		}
	}

	@PostMapping("/staff/cards/{requestNumber}/approve")
	public String approve(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String requestNumber,
			@RequestParam(required = false) String note, RedirectAttributes redirectAttributes) {
		return action(requestNumber, redirectAttributes,
				() -> cardService.approve(userDetails.getUsername(), requestNumber, note), "Card request approved.");
	}

	@PostMapping("/staff/cards/{requestNumber}/reject")
	public String reject(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String requestNumber,
			@RequestParam String reason, RedirectAttributes redirectAttributes) {
		return action(requestNumber, redirectAttributes,
				() -> cardService.reject(userDetails.getUsername(), requestNumber, reason), "Card request rejected.");
	}

	@PostMapping("/staff/cards/{requestNumber}/block")
	public String block(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String requestNumber,
			@RequestParam String reason, RedirectAttributes redirectAttributes) {
		return action(requestNumber, redirectAttributes,
				() -> cardService.blockByStaff(userDetails.getUsername(), requestNumber, reason), "Card blocked.");
	}

	@PostMapping("/staff/cards/{requestNumber}/reactivate")
	public String reactivate(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String requestNumber,
			@RequestParam(required = false) String note, RedirectAttributes redirectAttributes) {
		return action(requestNumber, redirectAttributes,
				() -> cardService.reactivateByStaff(userDetails.getUsername(), requestNumber, note), "Card reactivated.");
	}

	private String action(String requestNumber, RedirectAttributes redirectAttributes, Runnable action,
			String successMessage) {
		try {
			action.run();
			redirectAttributes.addFlashAttribute("successMessage", successMessage);
		}
		catch (CardException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/staff/cards/" + requestNumber;
	}
}
