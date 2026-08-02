package com.digibank.controller;

import com.digibank.dto.transfer.InternalAccountLookupView;
import com.digibank.dto.transfer.TransferDetailsView;
import com.digibank.dto.transfer.TransferFormView;
import com.digibank.dto.transfer.TransferRequest;
import com.digibank.enums.TransferRecipientType;
import com.digibank.enums.TransferStatus;
import com.digibank.util.PageSupport;
import java.util.Locale;
import com.digibank.exception.TransferException;
import com.digibank.exception.TransferNotFoundException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.TransferService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer/transfers")
public class CustomerTransferController {

	private final TransferService transferService;

	public CustomerTransferController(TransferService transferService) {
		this.transferService = transferService;
	}

	@GetMapping
	public String history(@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam(required=false) String q, @RequestParam(required=false) TransferStatus status,
			@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size, Model model) {
		String query=PageSupport.query(q);
		var filtered=transferService.getTransferHistory(userDetails.getUserId()).stream()
				.filter(t->status==null||t.status()==status)
				.filter(t->query.isEmpty()||t.referenceNumber().toLowerCase(Locale.ROOT).contains(query)
						||t.beneficiaryName().toLowerCase(Locale.ROOT).contains(query)||t.destinationBank().toLowerCase(Locale.ROOT).contains(query)).toList();
		var result=PageSupport.page(filtered,page,size);model.addAttribute("transfers",result.getContent());model.addAttribute("transfersPage",result);
		model.addAttribute("query",q==null?"":q.trim());model.addAttribute("selectedStatus",status);model.addAttribute("transferStatuses",TransferStatus.values());
		return "customer/transfers/history";
	}

	@GetMapping("/new")
	public String transferForm(@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam(required = false) Long beneficiaryId, Model model) {
		TransferFormView form = transferService.getTransferForm(userDetails.getUserId());
		if (!model.containsAttribute("transferRequest")) {
			TransferRequest request = new TransferRequest();
			request.setBeneficiaryId(beneficiaryId);
			if (beneficiaryId == null && form.beneficiaries().isEmpty()) {
				request.setRecipientType(TransferRecipientType.DIGIBANK_ACCOUNT);
			}
			model.addAttribute("transferRequest", request);
		}
		model.addAttribute("transferForm", form);
		return "customer/transfers/new";
	}

	@PostMapping("/internal-account-lookup")
	@ResponseBody
	public InternalAccountLookupView lookupInternalAccount(@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam String accountNumber) {
		return transferService.lookupInternalAccount(userDetails.getUserId(), accountNumber);
	}

	@PostMapping
	public String transfer(@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @ModelAttribute("transferRequest") TransferRequest request, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			addFormOptions(userDetails, model);
			return "customer/transfers/new";
		}
		try {
			TransferDetailsView completed = transferService.transfer(userDetails.getUserId(), userDetails.getUsername(),
					request);
			redirectAttributes.addFlashAttribute("successMessage", "Transfer completed successfully.");
			return "redirect:/customer/transfers/" + completed.referenceNumber();
		}
		catch (TransferException ex) {
			bindingResult.reject("transfer.failed", ex.getMessage());
			request.setTransactionPin(null);
			addFormOptions(userDetails, model);
			return "customer/transfers/new";
		}
	}

	@GetMapping("/{referenceNumber}")
	public String details(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable String referenceNumber, Model model) {
		model.addAttribute("transfer", transferService.getTransfer(userDetails.getUserId(), referenceNumber));
		return "customer/transfers/details";
	}

	@ExceptionHandler(TransferNotFoundException.class)
	public String transferNotFound(Model model) {
		model.addAttribute("errorMessage", "That transfer record is not available for your DigiBank profile.");
		return "customer/access-denied";
	}

	private void addFormOptions(CustomUserDetails userDetails, Model model) {
		model.addAttribute("transferForm", transferService.getTransferForm(userDetails.getUserId()));
	}
}
