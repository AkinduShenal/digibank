package com.digibank.controller;

import com.digibank.dto.schedule.*;
import com.digibank.enums.*;
import com.digibank.exception.ScheduledPaymentException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;

@Controller @RequestMapping("/customer/schedules")
public class CustomerScheduledPaymentController {
	private final ScheduledPaymentService schedules; private final TransferService transfers; private final BillPaymentService bills;
	public CustomerScheduledPaymentController(ScheduledPaymentService schedules,TransferService transfers,BillPaymentService bills){this.schedules=schedules;this.transfers=transfers;this.bills=bills;}
	@GetMapping public String list(@AuthenticationPrincipal CustomUserDetails u,Model m){m.addAttribute("schedules",schedules.list(u.getUserId()));return "customer/schedules/list";}
	@GetMapping("/new") public String form(@AuthenticationPrincipal CustomUserDetails u,Model m){
		if(!m.containsAttribute("scheduleRequest")){ScheduledPaymentRequest r=new ScheduledPaymentRequest();r.setNextExecutionAt(LocalDateTime.now().plusDays(1).withSecond(0).withNano(0));m.addAttribute("scheduleRequest",r);}return addOptions(u,m,"customer/schedules/new");}
	@PostMapping public String create(@AuthenticationPrincipal CustomUserDetails u,@Valid @ModelAttribute("scheduleRequest") ScheduledPaymentRequest r,BindingResult result,Model m,RedirectAttributes redirect){
		if(result.hasErrors())return addOptions(u,m,"customer/schedules/new");
		try{var created=schedules.create(u.getUserId(),u.getUsername(),r);redirect.addFlashAttribute("successMessage","Scheduled payment created.");return "redirect:/customer/schedules/"+created.reference();}
		catch(ScheduledPaymentException ex){m.addAttribute("errorMessage",ex.getMessage());r.setTransactionPin(null);return addOptions(u,m,"customer/schedules/new");}}
	@GetMapping("/{reference}") public String details(@AuthenticationPrincipal CustomUserDetails u,@PathVariable String reference,Model m){m.addAttribute("schedule",schedules.get(u.getUserId(),reference));return "customer/schedules/details";}
	@GetMapping("/{reference}/edit") public String edit(@AuthenticationPrincipal CustomUserDetails u,@PathVariable String reference,Model m){m.addAttribute("reference",reference);if(!m.containsAttribute("updateRequest"))m.addAttribute("updateRequest",schedules.getUpdateRequest(u.getUserId(),reference));return "customer/schedules/edit";}
	@PostMapping("/{reference}/edit") public String update(@AuthenticationPrincipal CustomUserDetails u,@PathVariable String reference,@Valid @ModelAttribute("updateRequest") ScheduledPaymentUpdateRequest r,BindingResult result,Model m,RedirectAttributes redirect){
		if(result.hasErrors()){m.addAttribute("reference",reference);return "customer/schedules/edit";}try{schedules.update(u.getUserId(),u.getUsername(),reference,r);redirect.addFlashAttribute("successMessage","Scheduled payment updated.");}catch(ScheduledPaymentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/customer/schedules/"+reference;}
	@PostMapping("/{reference}/cancel") public String cancel(@AuthenticationPrincipal CustomUserDetails u,@PathVariable String reference,RedirectAttributes redirect){try{schedules.cancel(u.getUserId(),u.getUsername(),reference);redirect.addFlashAttribute("successMessage","Scheduled payment cancelled.");}catch(ScheduledPaymentException ex){redirect.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/customer/schedules/"+reference;}
	private String addOptions(CustomUserDetails u,Model m,String view){m.addAttribute("transferForm",transfers.getTransferForm(u.getUserId()));m.addAttribute("billForm",bills.getPaymentForm(u.getUserId()));m.addAttribute("paymentTypes",ScheduledPaymentType.values());m.addAttribute("recurrences",ScheduleRecurrence.values());m.addAttribute("recipientTypes",TransferRecipientType.values());m.addAttribute("billerTypes",BillerSelectionType.values());return view;}
}
