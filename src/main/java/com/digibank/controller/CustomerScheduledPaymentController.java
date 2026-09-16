package com.digibank.controller;

import com.digibank.dto.schedule.*;
import com.digibank.enums.*;
import com.digibank.exception.ScheduledPaymentException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;

@Controller
@RequestMapping({"/customer/transfers/schedules", "/customer/bill-payments/schedules"})
public class CustomerScheduledPaymentController {
    private final ScheduledPaymentService schedules;
    private final TransferService transfers;
    private final BillPaymentService bills;

    public CustomerScheduledPaymentController(ScheduledPaymentService schedules, TransferService transfers,
            BillPaymentService bills) {
        this.schedules = schedules;
        this.transfers = transfers;
        this.bills = bills;
    }

    static ScheduledPaymentType type(String module) {
        return "transfers".equals(module) ? ScheduledPaymentType.FUND_TRANSFER : ScheduledPaymentType.BILL_PAYMENT;
    }

    static String basePath(ScheduledPaymentType type) {
        return "/customer/" + (type == ScheduledPaymentType.FUND_TRANSFER ? "transfers" : "bill-payments") + "/schedules";
    }

    private String module(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length()).startsWith("/customer/transfers/") ? "transfers" : "bill-payments";
    }

    @ModelAttribute
    void moduleContext(HttpServletRequest request, Model model) {
        String module = module(request);
        request.setAttribute("scheduleModule", module);
        boolean transfer = type(module) == ScheduledPaymentType.FUND_TRANSFER;
        model.addAttribute("isTransfer", transfer);
        model.addAttribute("basePath", basePath(type(module)));
        model.addAttribute("modulePath", "/customer/" + module);
        model.addAttribute("scheduleTitle", transfer ? "Scheduled transfers" : "Scheduled bill payments");
        model.addAttribute("itemLabel", transfer ? "transfer" : "bill payment");
    }

    @ModelAttribute("scheduleRequest")
    ScheduledPaymentRequest newRequest(HttpServletRequest servletRequest) {
        String module = module(servletRequest);
        ScheduledPaymentRequest request = new ScheduledPaymentRequest();
        request.setPaymentType(type(module));
        request.setTransferRecipientType(TransferRecipientType.DIGIBANK_ACCOUNT);
        request.setBillerSelectionType(BillerSelectionType.NEW_BILLER);
        request.setNextExecutionAt(LocalDateTime.now().plusDays(1).withSecond(0).withNano(0));
        return request;
    }

    @InitBinder("scheduleRequest")
    void bindSchedule(WebDataBinder binder) {
        // The route defines the module; a submitted field cannot change it.
        binder.setDisallowedFields("paymentType");
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails user, @RequestAttribute("scheduleModule") String module, Model model) {
        model.addAttribute("schedules", schedules.list(user.getUserId(), type(module)));
        return "customer/schedules/list";
    }

    @GetMapping("/new")
    public String form(@AuthenticationPrincipal CustomUserDetails user, @RequestAttribute("scheduleModule") String module, Model model) {
        return addOptions(user, module, model);
    }

    @PostMapping
    public String create(@AuthenticationPrincipal CustomUserDetails user, @RequestAttribute("scheduleModule") String module,
            @Valid @ModelAttribute("scheduleRequest") ScheduledPaymentRequest request, BindingResult result,
            Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                var created = schedules.create(user.getUserId(), user.getUsername(), request);
                redirect.addFlashAttribute("successMessage", "Schedule created. You can edit or cancel it before it runs.");
                return "redirect:" + basePath(type(module)) + "/" + created.reference();
            } catch (ScheduledPaymentException ex) {
                model.addAttribute("errorMessage", ex.getMessage());
            }
        }
        request.setTransactionPin(null);
        return addOptions(user, module, model);
    }

    @GetMapping("/{reference}")
    public String details(@AuthenticationPrincipal CustomUserDetails user, @RequestAttribute("scheduleModule") String module,
            @PathVariable String reference, Model model) {
        model.addAttribute("schedule", ownedSchedule(user, module, reference));
        return "customer/schedules/details";
    }

    @GetMapping("/{reference}/edit")
    public String edit(@AuthenticationPrincipal CustomUserDetails user, @RequestAttribute("scheduleModule") String module,
            @PathVariable String reference, Model model, RedirectAttributes redirect) {
        model.addAttribute("schedule", ownedSchedule(user, module, reference));
        try {
            model.addAttribute("updateRequest", schedules.getUpdateRequest(user.getUserId(), reference));
            return "customer/schedules/edit";
        } catch (ScheduledPaymentException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + basePath(type(module)) + "/" + reference;
        }
    }

    @PostMapping("/{reference}/edit")
    public String update(@AuthenticationPrincipal CustomUserDetails user, @RequestAttribute("scheduleModule") String module,
            @PathVariable String reference, @Valid @ModelAttribute("updateRequest") ScheduledPaymentUpdateRequest request,
            BindingResult result, Model model, RedirectAttributes redirect) {
        model.addAttribute("schedule", ownedSchedule(user, module, reference));
        if (!result.hasErrors()) {
            try {
                schedules.update(user.getUserId(), user.getUsername(), reference, request);
                redirect.addFlashAttribute("successMessage", "Schedule updated successfully.");
                return "redirect:" + basePath(type(module)) + "/" + reference;
            } catch (ScheduledPaymentException ex) {
                model.addAttribute("errorMessage", ex.getMessage());
            }
        }
        return "customer/schedules/edit";
    }

    @PostMapping("/{reference}/cancel")
    public String cancel(@AuthenticationPrincipal CustomUserDetails user, @RequestAttribute("scheduleModule") String module,
            @PathVariable String reference, RedirectAttributes redirect) {
        ownedSchedule(user, module, reference);
		try {
			schedules.cancel(user.getUserId(), user.getUsername(), reference);
			redirect.addFlashAttribute("successMessage", "Schedule cancelled and removed from your scheduled payments.");
		} catch (ScheduledPaymentException ex) {
			redirect.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:" + basePath(type(module));
	}

    private ScheduledPaymentView ownedSchedule(CustomUserDetails user, String module, String reference) {
        try {
            var schedule = schedules.get(user.getUserId(), reference);
            if (schedule.paymentType() == type(module)) return schedule;
        } catch (ScheduledPaymentException ignored) {
            // Missing, another customer's, and wrong-module references get the same response.
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found.");
    }

    private String addOptions(CustomUserDetails user, String module, Model model) {
        if (type(module) == ScheduledPaymentType.FUND_TRANSFER) {
            var form = transfers.getTransferForm(user.getUserId());
            model.addAttribute("transferForm", form);
            model.addAttribute("accounts", form.accounts());
        } else {
            var form = bills.getPaymentForm(user.getUserId());
            model.addAttribute("billForm", form);
            model.addAttribute("accounts", form.accounts());
        }
        return "customer/schedules/new";
    }
}
