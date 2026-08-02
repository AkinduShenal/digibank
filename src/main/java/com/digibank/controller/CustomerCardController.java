package com.digibank.controller;

import com.digibank.dto.card.CardActionRequest;
import com.digibank.dto.card.CardNumberRevealView;
import com.digibank.dto.card.CardLimitRequest;
import com.digibank.dto.card.CardRequest;
import com.digibank.exception.CardException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.CardManagementService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.digibank.enums.CardStatus;
import com.digibank.enums.CardType;
import com.digibank.util.PageSupport;
import java.util.Locale;

@Controller
public class CustomerCardController {

	private final CardManagementService cardService;

	public CustomerCardController(CardManagementService cardService) {
		this.cardService = cardService;
	}

	@GetMapping("/customer/cards")
	public String cards(@AuthenticationPrincipal CustomUserDetails userDetails,@RequestParam(required=false) String q,
			@RequestParam(required=false) CardStatus status,@RequestParam(required=false) CardType type,
			@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="8") int size,Model model) {
		String query=PageSupport.query(q);var filtered=cardService.getCustomerCards(userDetails.getUserId()).stream()
				.filter(c->status==null||c.status()==status).filter(c->type==null||c.cardType()==type)
				.filter(c->query.isEmpty()||c.requestNumber().toLowerCase(Locale.ROOT).contains(query)||c.cardType().getDisplayName().toLowerCase(Locale.ROOT).contains(query)).toList();var result=PageSupport.page(filtered,page,size);
		model.addAttribute("cards",result.getContent());model.addAttribute("cardsPage",result);model.addAttribute("query",q==null?"":q.trim());model.addAttribute("selectedStatus",status);model.addAttribute("selectedType",type);model.addAttribute("cardStatuses",CardStatus.values());model.addAttribute("cardTypes",CardType.values());
		return "customer/cards/list";
	}

	@GetMapping("/customer/cards/new")
	public String newCard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		if (!model.containsAttribute("cardRequest")) {
			model.addAttribute("cardRequest", new CardRequest());
		}
		model.addAttribute("form", cardService.getRequestForm(userDetails.getUserId()));
		return "customer/cards/request";
	}

	@PostMapping("/customer/cards")
	public String request(@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @ModelAttribute CardRequest cardRequest, BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("form", cardService.getRequestForm(userDetails.getUserId()));
			return "customer/cards/request";
		}
		try {
			var card = cardService.requestCard(userDetails.getUserId(), userDetails.getUsername(), cardRequest);
			redirectAttributes.addFlashAttribute("successMessage", "Card request submitted successfully.");
			return "redirect:/customer/cards/" + card.requestNumber();
		}
		catch (CardException ex) {
			model.addAttribute("errorMessage", ex.getMessage());
			model.addAttribute("form", cardService.getRequestForm(userDetails.getUserId()));
			return "customer/cards/request";
		}
	}

	@GetMapping("/customer/cards/{requestNumber}")
	public String details(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String requestNumber,
			Model model, RedirectAttributes redirectAttributes) {
		try {
			model.addAttribute("card", cardService.getCustomerCard(userDetails.getUserId(), requestNumber));
			model.addAttribute("cardActionRequest", new CardActionRequest());
			return "customer/cards/details";
		}
		catch (CardException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/customer/cards";
		}
	}

	@PostMapping("/customer/cards/{requestNumber}/number")
	@ResponseBody
	public ResponseEntity<?> revealNumber(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable String requestNumber) {
		try {
			String number = cardService.revealCardNumber(userDetails.getUserId(), userDetails.getUsername(), requestNumber);
			return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new CardNumberRevealView(number));
		}
		catch (CardException ex) {
			return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
					.body(java.util.Map.of("message", ex.getMessage()));
		}
	}

	@PostMapping("/customer/cards/{requestNumber}/activate")
	public String activate(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String requestNumber,
			@Valid @ModelAttribute CardActionRequest cardActionRequest, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		return action(userDetails, requestNumber, cardActionRequest, bindingResult, redirectAttributes, "activate");
	}

	@PostMapping("/customer/cards/{requestNumber}/block")
	public String block(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String requestNumber,
			@Valid @ModelAttribute CardActionRequest cardActionRequest, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		return action(userDetails, requestNumber, cardActionRequest, bindingResult, redirectAttributes, "block");
	}

	@PostMapping("/customer/cards/{requestNumber}/reactivate")
	public String reactivate(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String requestNumber,
			@Valid @ModelAttribute CardActionRequest cardActionRequest, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		return action(userDetails, requestNumber, cardActionRequest, bindingResult, redirectAttributes, "reactivate");
	}

	@PostMapping("/customer/cards/{requestNumber}/limit")
	public String updateLimit(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String requestNumber,
			@Valid @ModelAttribute CardLimitRequest request, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			return "redirect:/customer/cards/" + requestNumber;
		}
		try {
			cardService.updateSpendingLimit(userDetails.getUserId(), userDetails.getUsername(), requestNumber,
					request.getSpendingLimit(), request.getTransactionPin());
			redirectAttributes.addFlashAttribute("successMessage", "Card spending limit updated.");
		} catch (CardException ex) { redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage()); }
		return "redirect:/customer/cards/" + requestNumber;
	}

	@PostMapping("/customer/cards/{requestNumber}/lost-stolen")
	public String lostOrStolen(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String requestNumber,
			@RequestParam String reason, @RequestParam String transactionPin, RedirectAttributes redirectAttributes) {
		try {
			cardService.reportLostOrStolen(userDetails.getUserId(), userDetails.getUsername(), requestNumber, reason,
					transactionPin);
			redirectAttributes.addFlashAttribute("successMessage", "Card secured and marked as lost or stolen.");
		} catch (CardException ex) { redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage()); }
		return "redirect:/customer/cards/" + requestNumber;
	}

	private String action(CustomUserDetails userDetails, String requestNumber, CardActionRequest request,
			BindingResult bindingResult, RedirectAttributes redirectAttributes, String action) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("errorMessage",
					bindingResult.getAllErrors().getFirst().getDefaultMessage());
			return "redirect:/customer/cards/" + requestNumber;
		}
		try {
			switch (action) {
				case "activate" -> cardService.activate(userDetails.getUserId(), userDetails.getUsername(), requestNumber,
						request.getTransactionPin());
				case "block" -> cardService.blockByCustomer(userDetails.getUserId(), userDetails.getUsername(), requestNumber,
						request.getTransactionPin());
				case "reactivate" -> cardService.reactivateByCustomer(userDetails.getUserId(), userDetails.getUsername(),
						requestNumber, request.getTransactionPin());
				default -> throw new CardException("Unsupported card action.");
			}
			redirectAttributes.addFlashAttribute("successMessage", "Card status updated successfully.");
		}
		catch (CardException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/customer/cards/" + requestNumber;
	}
}
