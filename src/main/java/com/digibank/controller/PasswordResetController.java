package com.digibank.controller;

import com.digibank.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {
	private final PasswordResetService resets; private final boolean showLink;
	public PasswordResetController(PasswordResetService resets,@Value("${digibank.password-reset.show-link:true}") boolean showLink){this.resets=resets;this.showLink=showLink;}
	@GetMapping("/forgot-password") public String requestForm(){return "auth/forgot-password";}
	@PostMapping("/forgot-password") public String request(@RequestParam String identifier,Model model){String token=resets.requestToken(identifier);model.addAttribute("requested",true);if(showLink&&token!=null)model.addAttribute("resetToken",token);return "auth/forgot-password";}
	@GetMapping("/reset-password") public String resetForm(@RequestParam String token,Model model){model.addAttribute("token",token);model.addAttribute("valid",resets.isTokenValid(token));return "auth/reset-password";}
	@PostMapping("/reset-password") public String reset(@RequestParam String token,@RequestParam String password,@RequestParam String confirmation,Model model,RedirectAttributes redirect){try{resets.reset(token,password,confirmation);redirect.addFlashAttribute("resetSuccess",true);return "redirect:/login";}catch(IllegalArgumentException ex){model.addAttribute("token",token);model.addAttribute("valid",resets.isTokenValid(token));model.addAttribute("errorMessage",ex.getMessage());return "auth/reset-password";}}
}
