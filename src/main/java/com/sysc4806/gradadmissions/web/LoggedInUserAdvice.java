package com.sysc4806.gradadmissions.web;

import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.sysc4806.gradadmissions.security.AppUserDetails;
import com.sysc4806.gradadmissions.security.Role;

/**
 * Puts {@code loggedInUser} in the model of every page, for the nav bar and the home page.
 * It reads only the login (no database query), so {@code @WebMvcTest}s need no extra mocks.
 */
@ControllerAdvice
public class LoggedInUserAdvice {

    /**
     * @param name     the user's name (the email for {@code @WithMockUser} test users)
     * @param role     "ADMIN", "PROFESSOR" or "APPLICANT", compared as a string in templates
     * @param roleLabel "Admin", "Professor" or "Applicant"
     */
    public record LoggedInUser(String name, String role, String roleLabel) {}

    /** Null when nobody is logged in. */
    @ModelAttribute("loggedInUser")
    public LoggedInUser loggedInUser(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            return null;
        }
        String name = authentication.getPrincipal() instanceof AppUserDetails details
                ? details.getDisplayName()
                : authentication.getName();
        return Role.fromAuthorities(authentication.getAuthorities())
                .map(role -> new LoggedInUser(name, role.name(), role.label()))
                .orElse(new LoggedInUser(name, null, null));
    }
}
