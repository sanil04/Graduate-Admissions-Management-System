package com.sysc4806.gradadmissions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.sysc4806.gradadmissions.config.DataSeeder;
import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.repository.ApplicantRepository;

/** Logs in through the real login form with the seeded accounts (README "Demo logins"). */
@SpringBootTest
@AutoConfigureMockMvc
class LoginTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private ApplicantRepository applicants;

	@Test
	void loginPageIsPublicAndHasTheCsrfToken() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("name=\"email\"")))
				.andExpect(content().string(containsString("name=\"password\"")))
				.andExpect(content().string(containsString("name=\"_csrf\"")));
	}

	@Test
	void eachSeededRoleLogsInAndLandsOnItsSection() throws Exception {
		mockMvc.perform(formLogin("/login").userParameter("email")
						.user(DataSeeder.ADMIN_EMAIL).password(DataSeeder.ADMIN_PASSWORD))
				.andExpect(redirectedUrl("/admin"))
				.andExpect(authenticated().withRoles("ADMIN"));

		mockMvc.perform(formLogin("/login").userParameter("email")
						.user("achen@gradadmissions.local").password(DataSeeder.PROFESSOR_PASSWORD))
				.andExpect(redirectedUrl("/professor"))
				.andExpect(authenticated().withRoles("PROFESSOR"));

		mockMvc.perform(formLogin("/login").userParameter("email")
						.user(DataSeeder.APPLICANT_EMAIL).password(DataSeeder.APPLICANT_PASSWORD))
				.andExpect(redirectedUrl("/applicant"))
				.andExpect(authenticated().withRoles("APPLICANT"));
	}

	@Test
	void wrongPasswordIsRejected() throws Exception {
		mockMvc.perform(formLogin("/login").userParameter("email")
						.user(DataSeeder.ADMIN_EMAIL).password("wrong"))
				.andExpect(redirectedUrl("/login?error"))
				.andExpect(unauthenticated());

		mockMvc.perform(get("/login").param("error", ""))
				.andExpect(content().string(containsString("Wrong email or password")));
	}

	@Test
	void unknownEmailIsRejected() throws Exception {
		mockMvc.perform(formLogin("/login").userParameter("email")
						.user("nobody@example.com").password("anything"))
				.andExpect(redirectedUrl("/login?error"))
				.andExpect(unauthenticated());
	}

	@Test
	void aUserWithoutAPasswordCannotLogIn() throws Exception {
		// Like the applicants the application form created before login existed.
		applicants.save(new Applicant("No Password", "no.password@example.com"));

		mockMvc.perform(formLogin("/login").userParameter("email")
						.user("no.password@example.com").password(""))
				.andExpect(redirectedUrl("/login?error"))
				.andExpect(unauthenticated());
	}

	@Test
	void loginReturnsToThePageThatAskedForIt() throws Exception {
		MockHttpSession session = new MockHttpSession();
		mockMvc.perform(get("/admin/applications").session(session))
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(post("/login").session(session).with(csrf())
						.param("email", DataSeeder.ADMIN_EMAIL)
						.param("password", DataSeeder.ADMIN_PASSWORD))
				.andExpect(redirectedUrlPattern("**/admin/applications*"));
	}

	@Test
	void logoutEndsTheSession() throws Exception {
		mockMvc.perform(logout())
				.andExpect(redirectedUrl("/login?logout"))
				.andExpect(unauthenticated());

		mockMvc.perform(get("/login").param("logout", ""))
				.andExpect(content().string(containsString("You have been logged out")));
	}
}
