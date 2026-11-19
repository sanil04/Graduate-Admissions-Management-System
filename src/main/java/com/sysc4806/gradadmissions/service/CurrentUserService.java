package com.sysc4806.gradadmissions.service;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.model.Admin;
import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.User;
import com.sysc4806.gradadmissions.repository.UserRepository;
import com.sysc4806.gradadmissions.security.AppUserDetails;

/**
 * The logged-in user, as an entity. Typical use in a controller:
 * <pre>{@code
 * Professor me = currentUserService.currentProfessor();
 * }</pre>
 * The {@code currentX()} methods throw {@link AccessDeniedException} (HTTP 403, or a redirect to
 * the login page when nobody is logged in) if the user is not logged in with that role.
 * In {@code @WebMvcTest}s, mock this service with {@code @MockitoBean}.
 */
@Service
public class CurrentUserService {

	private final UserRepository users;

	public CurrentUserService(UserRepository users) {
		this.users = users;
	}

	/** The logged-in user, or empty when nobody is logged in. */
	@Transactional(readOnly = true)
	public Optional<User> currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}
		if (authentication.getPrincipal() instanceof AppUserDetails details) {
			return users.findById(details.getUserId());
		}
		// e.g. @WithMockUser(username = "achen@gradadmissions.local") in tests
		return users.findByEmail(authentication.getName());
	}

	public Admin currentAdmin() {
		return current(Admin.class);
	}

	public Professor currentProfessor() {
		return current(Professor.class);
	}

	public Applicant currentApplicant() {
		return current(Applicant.class);
	}

	private <T extends User> T current(Class<T> type) {
		return currentUser()
				.filter(type::isInstance)
				.map(type::cast)
				.orElseThrow(() -> new AccessDeniedException(
						"You must be logged in as " + type.getSimpleName().toLowerCase() + " to do this"));
	}
}
