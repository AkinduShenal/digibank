package com.digibank.controller;

import com.digibank.dto.auth.CustomerRegistrationRequest;
import com.digibank.dto.auth.RegistrationResult;
import com.digibank.enums.AccountType;
import com.digibank.enums.Gender;
import com.digibank.enums.IdentityType;
import com.digibank.exception.DuplicateEmailException;
import com.digibank.exception.DuplicateIdentityException;
import com.digibank.exception.DuplicateUsernameException;
import com.digibank.exception.InvalidInitialDepositException;
import com.digibank.exception.PasswordMismatchException;
import com.digibank.exception.PinMismatchException;
import com.digibank.exception.UnderageCustomerException;
import com.digibank.service.CustomerRegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class AuthController {

	private final CustomerRegistrationService customerRegistrationService;

	public AuthController(CustomerRegistrationService customerRegistrationService) {
		this.customerRegistrationService = customerRegistrationService;
	}

	@GetMapping("/login")
	public String login() {
		return "auth/login";
	}

	@GetMapping("/open-account")
	public String openAccount(Model model) {
		if (!model.containsAttribute("registrationRequest")) {
			model.addAttribute("registrationRequest", new CustomerRegistrationRequest());
		}
		addRegistrationOptions(model);
		return "auth/open-account";
	}

	@PostMapping("/open-account")
	public String register(@Valid @ModelAttribute("registrationRequest") CustomerRegistrationRequest request,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			addRegistrationOptions(model);
			return "auth/open-account";
		}
		try {
			RegistrationResult result = customerRegistrationService.register(request);
			redirectAttributes.addFlashAttribute("registrationResult", result);
			return "redirect:/registration-success";
		}
		catch (DuplicateUsernameException ex) {
			bindingResult.rejectValue("username", "duplicate", "This username is already registered.");
		}
		catch (DuplicateEmailException ex) {
			bindingResult.rejectValue("email", "duplicate", "This email is already registered.");
		}
		catch (DuplicateIdentityException ex) {
			bindingResult.rejectValue("identityNumber", "duplicate", "This NIC or passport number is already registered.");
		}
		catch (UnderageCustomerException ex) {
			bindingResult.rejectValue("dateOfBirth", "underage", ex.getMessage());
		}
		catch (InvalidInitialDepositException ex) {
			bindingResult.rejectValue("initialDeposit", "minimum", ex.getMessage());
		}
		catch (PasswordMismatchException ex) {
			bindingResult.rejectValue("confirmPassword", "mismatch", ex.getMessage());
		}
		catch (PinMismatchException ex) {
			bindingResult.rejectValue("confirmTransactionPin", "mismatch", ex.getMessage());
		}
		addRegistrationOptions(model);
		return "auth/open-account";
	}

	@GetMapping("/registration-success")
	public String registrationSuccess() {
		return "auth/registration-success";
	}

	@GetMapping("/access-denied")
	public String accessDenied() {
		return "auth/access-denied";
	}

	private void addRegistrationOptions(Model model) {
		model.addAttribute("genderOptions", Gender.values());
		model.addAttribute("identityTypeOptions", IdentityType.values());
		model.addAttribute("accountTypeOptions", new AccountType[] {AccountType.SAVINGS, AccountType.CURRENT});
		model.addAttribute("branchOptions", branchOptions());
	}

	private Map<String, String> branchOptions() {
		Map<String, String> branches = new LinkedHashMap<>();
		branches.put("COL001", "Colombo Main Branch");
		branches.put("KDY001", "Kandy Branch");
		branches.put("GAL001", "Galle Branch");
		branches.put("JAF001", "Jaffna Branch");
		branches.put("NEG001", "Negombo Branch");
		return branches;
	}
}
