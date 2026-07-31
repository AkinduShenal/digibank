package com.digibank.controller;

import com.digibank.dto.beneficiary.BeneficiaryCreateRequest;
import com.digibank.dto.beneficiary.BeneficiaryDetailsView;
import com.digibank.dto.beneficiary.BeneficiarySearchCriteria;
import com.digibank.dto.beneficiary.BeneficiaryUpdateRequest;
import com.digibank.enums.BeneficiaryAccountType;
import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;
import com.digibank.exception.BeneficiaryNotFoundException;
import com.digibank.exception.BeneficiaryVersionConflictException;
import com.digibank.exception.DuplicateBeneficiaryException;
import com.digibank.exception.InvalidBeneficiaryException;
import com.digibank.exception.InvalidBeneficiaryStateException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.BeneficiaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer/beneficiaries")
public class CustomerBeneficiaryController {

	private final BeneficiaryService beneficiaryService;

	public CustomerBeneficiaryController(BeneficiaryService beneficiaryService) {
		this.beneficiaryService = beneficiaryService;
	}

	@ModelAttribute("beneficiaryTypes")
	public BeneficiaryType[] beneficiaryTypes() {
		return BeneficiaryType.values();
	}

	@ModelAttribute("accountTypes")
	public BeneficiaryAccountType[] accountTypes() {
		return BeneficiaryAccountType.values();
	}

	@ModelAttribute("beneficiaryStatuses")
	public List<BeneficiaryStatus> beneficiaryStatuses() {
		return List.of(BeneficiaryStatus.ACTIVE, BeneficiaryStatus.INACTIVE);
	}

	@GetMapping
	public String list(@AuthenticationPrincipal CustomUserDetails userDetails,
			@ModelAttribute("criteria") BeneficiarySearchCriteria criteria, Model model) {
		model.addAttribute("beneficiaries", beneficiaryService.searchBeneficiaries(userId(userDetails), criteria));
		model.addAttribute("criteria", criteria);
		model.addAttribute("pageTitle", "Beneficiaries");
		return "customer/beneficiaries/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		if (!model.containsAttribute("beneficiaryCreateRequest")) {
			model.addAttribute("beneficiaryCreateRequest", new BeneficiaryCreateRequest());
		}
		model.addAttribute("pageTitle", "Add Beneficiary");
		return "customer/beneficiaries/create";
	}

	@PostMapping
	public String create(@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @ModelAttribute("beneficiaryCreateRequest") BeneficiaryCreateRequest request,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "Add Beneficiary");
			return "customer/beneficiaries/create";
		}
		try {
			BeneficiaryDetailsView created = beneficiaryService.createBeneficiary(userId(userDetails), request);
			redirectAttributes.addFlashAttribute("successMessage", "Beneficiary was added successfully.");
			return "redirect:/customer/beneficiaries/" + created.getId();
		}
		catch (DuplicateBeneficiaryException ex) {
			bindingResult.reject("beneficiary.duplicate", "This beneficiary already exists for your profile.");
			model.addAttribute("pageTitle", "Add Beneficiary");
			return "customer/beneficiaries/create";
		}
		catch (InvalidBeneficiaryException ex) {
			bindingResult.reject("beneficiary.invalid", safeMessage(ex));
			model.addAttribute("pageTitle", "Add Beneficiary");
			return "customer/beneficiaries/create";
		}
	}

	@GetMapping("/{beneficiaryId}")
	public String details(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long beneficiaryId, Model model) {
		model.addAttribute("beneficiary", beneficiaryService.getBeneficiary(userId(userDetails), beneficiaryId));
		model.addAttribute("pageTitle", "Beneficiary Details");
		return "customer/beneficiaries/details";
	}

	@GetMapping("/{beneficiaryId}/edit")
	public String editForm(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long beneficiaryId, Model model) {
		BeneficiaryDetailsView beneficiary = beneficiaryService.getBeneficiary(userId(userDetails), beneficiaryId);
		if (!model.containsAttribute("beneficiaryUpdateRequest")) {
			model.addAttribute("beneficiaryUpdateRequest", updateRequest(beneficiary));
		}
		model.addAttribute("beneficiary", beneficiary);
		model.addAttribute("pageTitle", "Edit Beneficiary");
		return "customer/beneficiaries/edit";
	}

	@PostMapping("/{beneficiaryId}/edit")
	public String update(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long beneficiaryId,
			@Valid @ModelAttribute("beneficiaryUpdateRequest") BeneficiaryUpdateRequest request,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			populateEditModel(userDetails, beneficiaryId, model);
			return "customer/beneficiaries/edit";
		}
		try {
			BeneficiaryDetailsView updated = beneficiaryService.updateBeneficiary(userId(userDetails), beneficiaryId,
					request);
			redirectAttributes.addFlashAttribute("successMessage", "Beneficiary was updated successfully.");
			return "redirect:/customer/beneficiaries/" + updated.getId();
		}
		catch (BeneficiaryVersionConflictException ex) {
			bindingResult.reject("beneficiary.version",
					"This beneficiary was updated in another session. Reload the page and try again.");
			populateEditModel(userDetails, beneficiaryId, model);
			return "customer/beneficiaries/edit";
		}
		catch (DuplicateBeneficiaryException ex) {
			bindingResult.reject("beneficiary.duplicate", "This beneficiary already exists for your profile.");
			populateEditModel(userDetails, beneficiaryId, model);
			return "customer/beneficiaries/edit";
		}
		catch (InvalidBeneficiaryException | InvalidBeneficiaryStateException ex) {
			bindingResult.reject("beneficiary.invalid", safeMessage(ex));
			populateEditModel(userDetails, beneficiaryId, model);
			return "customer/beneficiaries/edit";
		}
	}

	@PostMapping("/{beneficiaryId}/deactivate")
	public String deactivate(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long beneficiaryId,
			RedirectAttributes redirectAttributes) {
		return action(() -> beneficiaryService.deactivateBeneficiary(userId(userDetails), beneficiaryId),
				"Beneficiary was deactivated.", "redirect:/customer/beneficiaries/" + beneficiaryId,
				redirectAttributes);
	}

	@PostMapping("/{beneficiaryId}/reactivate")
	public String reactivate(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long beneficiaryId,
			RedirectAttributes redirectAttributes) {
		return action(() -> beneficiaryService.reactivateBeneficiary(userId(userDetails), beneficiaryId),
				"Beneficiary was reactivated.", "redirect:/customer/beneficiaries/" + beneficiaryId,
				redirectAttributes);
	}

	@PostMapping("/{beneficiaryId}/delete")
	public String delete(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long beneficiaryId,
			RedirectAttributes redirectAttributes) {
		try {
			beneficiaryService.deleteBeneficiary(userId(userDetails), beneficiaryId);
			redirectAttributes.addFlashAttribute("successMessage", "Beneficiary was removed from your list.");
		}
		catch (InvalidBeneficiaryStateException | InvalidBeneficiaryException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", safeMessage(ex));
		}
		return "redirect:/customer/beneficiaries";
	}

	@PostMapping("/{beneficiaryId}/favourite")
	public String favourite(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long beneficiaryId,
			RedirectAttributes redirectAttributes) {
		return action(() -> beneficiaryService.markFavourite(userId(userDetails), beneficiaryId),
				"Beneficiary was marked as favourite.", "redirect:/customer/beneficiaries/" + beneficiaryId,
				redirectAttributes);
	}

	@PostMapping("/{beneficiaryId}/unfavourite")
	public String unfavourite(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long beneficiaryId,
			RedirectAttributes redirectAttributes) {
		return action(() -> beneficiaryService.removeFavourite(userId(userDetails), beneficiaryId),
				"Beneficiary was removed from favourites.", "redirect:/customer/beneficiaries/" + beneficiaryId,
				redirectAttributes);
	}

	@ExceptionHandler(BeneficiaryNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleBeneficiaryNotFound(Model model) {
		model.addAttribute("errorMessage", "That beneficiary is not available for your DigiBank profile.");
		return "customer/access-denied";
	}

	private void populateEditModel(CustomUserDetails userDetails, Long beneficiaryId, Model model) {
		model.addAttribute("beneficiary", beneficiaryService.getBeneficiary(userId(userDetails), beneficiaryId));
		model.addAttribute("pageTitle", "Edit Beneficiary");
	}

	private BeneficiaryUpdateRequest updateRequest(BeneficiaryDetailsView beneficiary) {
		return new BeneficiaryUpdateRequest(beneficiary.getBeneficiaryName(), beneficiary.getNickname(),
				beneficiary.getBankName(), beneficiary.getBankCode(), beneficiary.getBranchName(),
				beneficiary.getBranchCode(), beneficiary.getAccountType(), beneficiary.getVersion());
	}

	private String action(BeneficiaryAction action, String successMessage, String redirect,
			RedirectAttributes redirectAttributes) {
		try {
			action.run();
			redirectAttributes.addFlashAttribute("successMessage", successMessage);
		}
		catch (InvalidBeneficiaryStateException | InvalidBeneficiaryException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", safeMessage(ex));
		}
		return redirect;
	}

	private Long userId(CustomUserDetails userDetails) {
		if (userDetails == null || userDetails.getUserId() == null) {
			throw new AccessDeniedException("Authenticated customer is required.");
		}
		return userDetails.getUserId();
	}

	private String safeMessage(RuntimeException ex) {
		String message = ex.getMessage();
		return message == null || message.isBlank() ? "Beneficiary request could not be completed." : message;
	}

	@FunctionalInterface
	private interface BeneficiaryAction {

		void run();
	}
}
