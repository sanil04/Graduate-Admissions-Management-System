package com.sysc4806.gradadmissions.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.sysc4806.gradadmissions.model.User;

/**
 * What Spring Security keeps in the session for a logged-in user. It stores the user's id, so the
 * user is still found after they change their email (the login name) on their profile.
 * Use {@link com.sysc4806.gradadmissions.service.CurrentUserService} to get the {@link User} entity.
 */
public class AppUserDetails extends org.springframework.security.core.userdetails.User {

	private final Long userId;
	private final String displayName;
	private final Role role;

	public AppUserDetails(User user) {
		super(user.getEmail(), user.getPasswordHash(),
				List.of(new SimpleGrantedAuthority(Role.of(user).authority())));
		this.userId = user.getId();
		this.displayName = user.getName();
		this.role = Role.of(user);
	}

	public Long getUserId() {
		return userId;
	}

	/** The user's name at login time, for the nav bar. */
	public String getDisplayName() {
		return displayName;
	}

	public Role getRole() {
		return role;
	}
}
