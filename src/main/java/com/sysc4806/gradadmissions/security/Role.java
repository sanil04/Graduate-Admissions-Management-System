package com.sysc4806.gradadmissions.security;

import java.util.Collection;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;

import com.sysc4806.gradadmissions.model.Admin;
import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.User;

/**
 * The three roles of the app. The role is not stored in the database: it is the {@link User}
 * subclass (JOINED inheritance already tells us which table the user is in).
 */
public enum Role {

	ADMIN("/admin"),
	PROFESSOR("/professor"),
	APPLICANT("/applicant");

	private final String homeUrl;

	Role(String homeUrl) {
		this.homeUrl = homeUrl;
	}

	/** The section a user of this role lands on after logging in. */
	public String homeUrl() {
		return homeUrl;
	}

	/** The Spring Security authority, e.g. {@code ROLE_ADMIN}, matched by {@code hasRole("ADMIN")}. */
	public String authority() {
		return "ROLE_" + name();
	}

	public static Role of(User user) {
		if (user instanceof Admin) {
			return ADMIN;
		}
		if (user instanceof Professor) {
			return PROFESSOR;
		}
		if (user instanceof Applicant) {
			return APPLICANT;
		}
		throw new IllegalArgumentException("Unknown kind of user: " + user.getClass().getSimpleName());
	}

	/** The first role found in the authorities of a logged-in user, if any. */
	public static Optional<Role> fromAuthorities(Collection<? extends GrantedAuthority> authorities) {
		for (Role role : values()) {
			for (GrantedAuthority authority : authorities) {
				if (role.authority().equals(authority.getAuthority())) {
					return Optional.of(role);
				}
			}
		}
		return Optional.empty();
	}
}
