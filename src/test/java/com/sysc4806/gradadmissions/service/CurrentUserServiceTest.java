package com.sysc4806.gradadmissions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sysc4806.gradadmissions.model.Admin;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.repository.UserRepository;
import com.sysc4806.gradadmissions.security.AppUserDetails;

class CurrentUserServiceTest {

	private UserRepository users;
	private CurrentUserService service;
	private Professor alice;

	@BeforeEach
	void setUp() {
		users = mock(UserRepository.class);
		service = new CurrentUserService(users);
		alice = new Professor("Alice Chen", "achen@gradadmissions.local");
		alice.setId(2L);
		alice.setPasswordHash("$2a$10$hash");
		when(users.findById(2L)).thenReturn(Optional.of(alice));
	}

	@AfterEach
	void clearLogin() {
		SecurityContextHolder.clearContext();
	}

	private void logIn(AppUserDetails principal) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

	@Test
	void nobodyLoggedInMeansNoCurrentUser() {
		assertThat(service.currentUser()).isEmpty();
		assertThatThrownBy(service::currentProfessor).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void anonymousVisitorsHaveNoCurrentUser() {
		SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
				"key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

		assertThat(service.currentUser()).isEmpty();
	}

	@Test
	void theLoggedInProfessorIsFoundById() {
		logIn(new AppUserDetails(alice));

		assertThat(service.currentUser()).contains(alice);
		assertThat(service.currentProfessor()).isSameAs(alice);
	}

	@Test
	void stillFoundAfterChangingTheirEmail() {
		logIn(new AppUserDetails(alice));
		alice.setEmail("alice.chen@carleton.ca");

		assertThat(service.currentProfessor()).isSameAs(alice);
	}

	@Test
	void askingForTheWrongRoleIsAccessDenied() {
		logIn(new AppUserDetails(alice));

		assertThatThrownBy(service::currentAdmin).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(service::currentApplicant).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void aMockUserIsLookedUpByEmail() {
		// What @WithMockUser(username = "...") puts in the security context.
		Admin admin = new Admin("System Admin", "admin@gradadmissions.local");
		when(users.findByEmail("admin@gradadmissions.local")).thenReturn(Optional.of(admin));
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"admin@gradadmissions.local", null, List.of()));

		assertThat(service.currentAdmin()).isSameAs(admin);
	}
}
