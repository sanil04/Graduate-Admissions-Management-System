package com.sysc4806.gradadmissions.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.repository.UserRepository;

/** Loads a user by email for the login form. Spring Security then checks the BCrypt password. */
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	public AppUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) {
		String trimmed = email.trim();
		// Exact match first: older accounts may differ only by case; registration stores lower case.
		return users.findByEmail(trimmed)
				.or(() -> users.findByEmailIgnoreCase(trimmed))
				// Users without a password (created before login existed) cannot log in.
				.filter(user -> user.getPasswordHash() != null)
				.map(AppUserDetails::new)
				.orElseThrow(() -> new UsernameNotFoundException("No account can log in as " + email));
	}
}
