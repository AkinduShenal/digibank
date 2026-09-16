package com.digibank.controller;

import com.digibank.exception.ScheduledPaymentException;
import com.digibank.security.CustomUserDetails;
import com.digibank.service.ScheduledPaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/customer/schedules")
public class LegacyScheduleController {
    private final ScheduledPaymentService schedules;

    public LegacyScheduleController(ScheduledPaymentService schedules) { this.schedules = schedules; }

    @GetMapping({"", "/new"})
    public String chooseModule() { return "customer/schedules/choose"; }

    @GetMapping({"/{reference}", "/{reference}/edit"})
    public String redirect(@AuthenticationPrincipal CustomUserDetails user, @PathVariable String reference,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            var schedule = schedules.get(user.getUserId(), reference);
            String suffix = request.getRequestURI().endsWith("/edit") ? "/edit" : "";
            return "redirect:" + CustomerScheduledPaymentController.basePath(schedule.paymentType()) + "/" + reference + suffix;
        } catch (ScheduledPaymentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found.");
        }
    }
}
