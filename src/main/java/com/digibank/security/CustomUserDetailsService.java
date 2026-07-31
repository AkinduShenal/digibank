package com.digibank.security;

import com.digibank.entity.Customer;
import com.digibank.entity.User;
import com.digibank.enums.CustomerStatus;
import com.digibank.repository.CustomerRepository;
import com.digibank.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;
	private final CustomerRepository customerRepository;

	public CustomUserDetailsService(UserRepository userRepository, CustomerRepository customerRepository) {
		this.userRepository = userRepository;
		this.customerRepository = customerRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
		String login = normalize(usernameOrEmail);
		User user = userRepository.findByUsernameIgnoreCase(login)
				.or(() -> userRepository.findByEmailIgnoreCase(login))
				.orElseThrow(() -> new UsernameNotFoundException("Invalid username or password."));
		CustomerStatus customerStatus = customerRepository.findByUserId(user.getId())
				.map(Customer::getStatus)
				.orElse(CustomerStatus.ACTIVE);
		return new CustomUserDetails(user, customerStatus);
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
