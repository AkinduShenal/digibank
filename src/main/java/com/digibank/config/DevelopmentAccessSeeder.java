package com.digibank.config;

import com.digibank.entity.User;
import com.digibank.enums.Role;
import com.digibank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "digibank.dev-seed.enabled", havingValue = "true")
public class DevelopmentAccessSeeder implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(DevelopmentAccessSeeder.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SeedUser staff;
	private final SeedUser admin;

	public DevelopmentAccessSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
			@Value("${digibank.dev-seed.staff.username}") String staffUsername,
			@Value("${digibank.dev-seed.staff.email}") String staffEmail,
			@Value("${digibank.dev-seed.staff.password}") String staffPassword,
			@Value("${digibank.dev-seed.admin.username}") String adminUsername,
			@Value("${digibank.dev-seed.admin.email}") String adminEmail,
			@Value("${digibank.dev-seed.admin.password}") String adminPassword) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.staff = new SeedUser(staffUsername, staffEmail, staffPassword, Role.BANK_STAFF);
		this.admin = new SeedUser(adminUsername, adminEmail, adminPassword, Role.ADMIN);
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		seed(staff);
		seed(admin);
	}

	private void seed(SeedUser seedUser) {
		String username = trim(seedUser.username());
		String email = trim(seedUser.email());
		String password = seedUser.password();
		if (username == null || email == null || password == null || password.isBlank()) {
			LOGGER.warn("Skipping {} development user seed because its credentials are incomplete.", seedUser.role());
			return;
		}
		if (userRepository.existsByUsernameIgnoreCase(username) || userRepository.existsByEmailIgnoreCase(email)) {
			LOGGER.info("Skipping {} development user seed because the username or email already exists.",
					seedUser.role());
			return;
		}
		User user = new User(username, email, passwordEncoder.encode(password));
		user.setTransactionPinHash(passwordEncoder.encode(UUID.randomUUID().toString()));
		user.setRole(seedUser.role());
		user.setEnabled(true);
		user.setAccountNonLocked(true);
		userRepository.save(user);
		LOGGER.info("Created development {} user with username '{}'.", seedUser.role(), username);
	}

	private String trim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record SeedUser(String username, String email, String password, Role role) {
	}
}
