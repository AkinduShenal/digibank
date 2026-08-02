package com.digibank.controller;

import com.digibank.dto.transaction.StatementExport;
import com.digibank.dto.transaction.TransactionSearchCriteria;
import com.digibank.exception.StatementException;
import com.digibank.exception.TransactionRecordNotFoundException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.AccountTransactionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer/transactions")
public class CustomerTransactionController {

	private final AccountTransactionService transactionService;

	public CustomerTransactionController(AccountTransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@GetMapping
	public String statement(@AuthenticationPrincipal CustomUserDetails userDetails,
			@ModelAttribute("criteria") TransactionSearchCriteria criteria, Model model) {
		model.addAttribute("statement", transactionService.getStatement(userDetails.getUserId(), criteria));
		return "customer/transactions/statement";
	}

	@GetMapping("/{transactionId}")
	public String details(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long transactionId, Model model) {
		model.addAttribute("transaction", transactionService.getTransaction(userDetails.getUserId(), transactionId));
		return "customer/transactions/details";
	}

	@GetMapping("/export/csv")
	public ResponseEntity<byte[]> csv(@AuthenticationPrincipal CustomUserDetails userDetails,
			@ModelAttribute TransactionSearchCriteria criteria) {
		return download(transactionService.exportCsv(userDetails.getUserId(), criteria));
	}

	@GetMapping("/export/pdf")
	public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal CustomUserDetails userDetails,
			@ModelAttribute TransactionSearchCriteria criteria) {
		return download(transactionService.exportPdf(userDetails.getUserId(), criteria));
	}

	@ExceptionHandler({TransactionRecordNotFoundException.class, StatementException.class})
	public String unavailable(RuntimeException exception, Model model) {
		model.addAttribute("errorMessage", exception.getMessage());
		return "customer/access-denied";
	}

	private ResponseEntity<byte[]> download(StatementExport export) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.header(HttpHeaders.CONTENT_TYPE, export.contentType())
				.body(export.content());
	}
}
