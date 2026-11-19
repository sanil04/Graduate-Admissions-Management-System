package com.sysc4806.gradadmissions.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * After login, go back to the page that asked for the login (if any), otherwise to the
 * section of the user's role: admins to /admin, professors to /professor, applicants to /applicant.
 */
public class RoleHomeSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	@Override
	protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) {
		return Role.fromAuthorities(authentication.getAuthorities()).map(Role::homeUrl).orElse("/");
	}
}
