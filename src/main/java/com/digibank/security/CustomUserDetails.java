package com.digibank.security;

import com.digibank.entity.User;
import com.digibank.enums.CustomerStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

	private final Long userId;
	private final String username;
	private final String passwordHash;
	private final boolean enabled;
	private final boolean accountNonLocked;
	private final CustomerStatus customerStatus;
	private final List<GrantedAuthority> authorities;

	public CustomUserDetails(User user, CustomerStatus customerStatus) {
		this.userId = user.getId();
		this.username = user.getUsername();
		this.passwordHash = user.getPasswordHash();
		this.enabled = user.isEnabled();
		this.accountNonLocked = user.isAccountNonLocked();
		this.customerStatus = customerStatus;
		this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

	public Long getUserId() {
		return userId;
	}

	public CustomerStatus getCustomerStatus() {
		return customerStatus;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return accountNonLocked && customerStatus != CustomerStatus.SUSPENDED;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled && customerStatus != CustomerStatus.DEACTIVATED
				&& customerStatus != CustomerStatus.PENDING_VERIFICATION;
	}
}
