package com.digibank.security;

import com.digibank.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final UserRepository userRepository;

	public CustomAuthenticationSuccessHandler(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		updateLastLogin(authentication);
		response.sendRedirect(targetUrl(authentication));
	}

	private void updateLastLogin(Authentication authentication) {
		Object principal = authentication.getPrincipal();
		if (principal instanceof CustomUserDetails userDetails && userDetails.getUserId() != null) {
			userRepository.updateLastLoginAt(userDetails.getUserId(), LocalDateTime.now());
		}
	}

	private String targetUrl(Authentication authentication) {
		if (hasRole(authentication, "ROLE_ADMIN")) {
			return "/admin/dashboard";
		}
		if (hasRole(authentication, "ROLE_BANK_STAFF")) {
			return "/staff/dashboard";
		}
		return "/customer/dashboard";
	}

	private boolean hasRole(Authentication authentication, String role) {
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (role.equals(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}
}
