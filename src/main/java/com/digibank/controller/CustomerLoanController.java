package com.digibank.controller;

import com.digibank.dto.loan.LoanApplicationRequest;
import com.digibank.dto.loan.LoanRepaymentRequest;
import com.digibank.exception.LoanException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.LoanManagementService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import com.digibank.enums.LoanStatus;
import com.digibank.util.PageSupport;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Locale;

@Controller
public class CustomerLoanController {

	private final LoanManagementService loanService;

	public CustomerLoanController(LoanManagementService loanService) {
		this.loanService = loanService;
	}

	@GetMapping("/customer/loans")
	public String loans(@AuthenticationPrincipal CustomUserDetails userDetails,@RequestParam(required=false) String q,
			@RequestParam(required=false) LoanStatus status,@RequestParam(defaultValue="0") int page,
			@RequestParam(defaultValue="9") int size,Model model) {
		String query=PageSupport.query(q);var filtered=loanService.getCustomerLoans(userDetails.getUserId()).stream()
				.filter(l->status==null||l.status()==status).filter(l->query.isEmpty()||l.applicationNumber().toLowerCase(Locale.ROOT).contains(query)
						||l.loanType().getDisplayName().toLowerCase(Locale.ROOT).contains(query)).toList();var result=PageSupport.page(filtered,page,size);
		model.addAttribute("loans",result.getContent());model.addAttribute("loansPage",result);model.addAttribute("query",q==null?"":q.trim());model.addAttribute("selectedStatus",status);model.addAttribute("loanStatuses",LoanStatus.values());
		return "customer/loans/list";
	}

	@GetMapping("/customer/loans/new")
	public String newLoan(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		if (!model.containsAttribute("loanApplicationRequest")) {
			model.addAttribute("loanApplicationRequest", new LoanApplicationRequest());
		}
		model.addAttribute("form", loanService.getApplicationForm(userDetails.getUserId()));
		return "customer/loans/apply";
	}

	@PostMapping("/customer/loans")
	public String apply(@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @ModelAttribute LoanApplicationRequest loanApplicationRequest, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("form", loanService.getApplicationForm(userDetails.getUserId()));
			return "customer/loans/apply";
		}
		try {
			var loan = loanService.apply(userDetails.getUserId(), userDetails.getUsername(), loanApplicationRequest);
			redirectAttributes.addFlashAttribute("successMessage", "Loan application submitted successfully.");
			return "redirect:/customer/loans/" + loan.applicationNumber();
		}
		catch (LoanException ex) {
			model.addAttribute("errorMessage", ex.getMessage());
			model.addAttribute("form", loanService.getApplicationForm(userDetails.getUserId()));
			return "customer/loans/apply";
		}
	}

	@GetMapping("/customer/loans/{applicationNumber}/edit")
	public String edit(@AuthenticationPrincipal CustomUserDetails userDetails,@PathVariable String applicationNumber,Model model,RedirectAttributes redirect){
		try{if(!model.containsAttribute("loanApplicationRequest"))model.addAttribute("loanApplicationRequest",loanService.getPendingApplicationForEdit(userDetails.getUserId(),applicationNumber));
			model.addAttribute("form",loanService.getApplicationForm(userDetails.getUserId()));model.addAttribute("applicationNumber",applicationNumber);return "customer/loans/edit";}
		catch(LoanException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());return "redirect:/customer/loans/"+applicationNumber;}}

	@GetMapping("/customer/loans/{applicationNumber}/document")
	public ResponseEntity<byte[]> document(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable String applicationNumber) {
		var document = loanService.getCustomerDocument(userDetails.getUserId(), applicationNumber);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				ContentDisposition.attachment().filename(document.filename(), StandardCharsets.UTF_8).build().toString())
				.header(HttpHeaders.CONTENT_TYPE, document.contentType()).body(document.content());
	}

	@PostMapping("/customer/loans/{applicationNumber}/edit")
	public String update(@AuthenticationPrincipal CustomUserDetails userDetails,@PathVariable String applicationNumber,
			@Valid @ModelAttribute LoanApplicationRequest request,BindingResult result,Model model,RedirectAttributes redirect){
		if(result.hasErrors()){model.addAttribute("form",loanService.getApplicationForm(userDetails.getUserId()));model.addAttribute("applicationNumber",applicationNumber);return "customer/loans/edit";}
		try{loanService.updatePendingApplication(userDetails.getUserId(),userDetails.getUsername(),applicationNumber,request);redirect.addFlashAttribute("successMessage","Loan application updated.");}
		catch(LoanException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/customer/loans/"+applicationNumber;}

	@PostMapping("/customer/loans/{applicationNumber}/withdraw")
	public String withdraw(@AuthenticationPrincipal CustomUserDetails userDetails,@PathVariable String applicationNumber,RedirectAttributes redirect){
		try{loanService.withdrawPendingApplication(userDetails.getUserId(),userDetails.getUsername(),applicationNumber);redirect.addFlashAttribute("successMessage","Loan application withdrawn.");}
		catch(LoanException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/customer/loans/"+applicationNumber;}

	@GetMapping("/customer/loans/{applicationNumber}")
	public String details(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable String applicationNumber, Model model, RedirectAttributes redirectAttributes) {
		try {
			model.addAttribute("loan", loanService.getCustomerLoan(userDetails.getUserId(), applicationNumber));
			model.addAttribute("repaymentAccounts", loanService.getRepaymentAccounts(userDetails.getUserId()));
			return "customer/loans/details";
		}
		catch (LoanException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/customer/loans";
		}
	}

	@PostMapping("/customer/loans/{applicationNumber}/repayments/{installmentNumber}")
	public String repay(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable String applicationNumber, @PathVariable int installmentNumber,
			@Valid @ModelAttribute LoanRepaymentRequest loanRepaymentRequest, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			String message = bindingResult.getAllErrors().getFirst().getDefaultMessage();
			redirectAttributes.addFlashAttribute("errorMessage", message);
			return "redirect:/customer/loans/" + applicationNumber;
		}
		try {
			String reference = loanService.payInstallment(userDetails.getUserId(), userDetails.getUsername(),
					applicationNumber, installmentNumber, loanRepaymentRequest);
			redirectAttributes.addFlashAttribute("successMessage",
					"Installment paid successfully. Reference: " + reference);
		}
		catch (LoanException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/customer/loans/" + applicationNumber;
	}
}
