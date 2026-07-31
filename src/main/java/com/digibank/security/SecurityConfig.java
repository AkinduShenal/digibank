package com.digibank.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;

	public SecurityConfig(CustomAuthenticationSuccessHandler authenticationSuccessHandler) {
		this.authenticationSuccessHandler = authenticationSuccessHandler;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/",
								"/login",
								"/open-account",
								"/registration-success",
								"/about",
								"/services",
								"/investment",
								"/contact",
								"/forgot-password",
								"/css/**",
								"/js/**",
								"/images/**",
								"/favicon/**",
								"/webjars/**")
						.permitAll()
						.requestMatchers("/customer/**", "/accounts/**", "/profile/**", "/notifications/**")
						.hasRole("CUSTOMER")
						.requestMatchers("/staff/**")
						.hasAnyRole("BANK_STAFF", "ADMIN")
						.requestMatchers("/admin/**")
						.hasRole("ADMIN")
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.loginProcessingUrl("/login")
						.usernameParameter("username")
						.passwordParameter("password")
						.successHandler(authenticationSuccessHandler)
						.failureHandler(authenticationFailureHandler())
						.permitAll())
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout")
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID")
						.permitAll())
				.build();
	}

	private AuthenticationFailureHandler authenticationFailureHandler() {
		return (request, response, exception) -> response.sendRedirect("/login?" + failureParameter(exception));
	}

	private String failureParameter(AuthenticationException exception) {
		if (exception instanceof DisabledException) {
			return "disabled";
		}
		if (exception instanceof LockedException) {
			return "locked";
		}
		return "error";
	}
}
