package com.sysc4806.gradadmissions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.sysc4806.gradadmissions.config.DataSeeder;
import com.sysc4806.gradadmissions.repository.UserRepository;

/** The URL rules of SecurityConfig, checked for every role against the whole app and the seeded users. */
@SpringBootTest
@AutoConfigureMockMvc
class AccessRulesTest {

	private static final String ALICE = "achen@gradadmissions.local";
	private static final String BRIAN = "bokonkwo@gradadmissions.local";

	/** One page of each section. */
	private static final Map<String, List<String>> PAGES_BY_ROLE = Map.of(
			"ADMIN", List.of("/admin", "/admin/applications"),
			"PROFESSOR", List.of("/professor", "/professor/profiles"),
			"APPLICANT", List.of("/applicant", "/apply"));

	/** Seeded users, so CurrentUserService finds them by email. */
	private static final Map<String, String> EMAIL_BY_ROLE = Map.of(
			"ADMIN", DataSeeder.ADMIN_EMAIL, "PROFESSOR", ALICE, "APPLICANT", DataSeeder.APPLICANT_EMAIL);

	@Autowired private MockMvc mockMvc;
	@Autowired private UserRepository users;

	private static RequestPostProcessor as(String role) {
		return user(EMAIL_BY_ROLE.get(role)).roles(role);
	}

	@Test
	void publicPagesNeedNoLogin() throws Exception {
		for (String page : List.of("/", "/login", "/css/style.css")) {
			mockMvc.perform(get(page)).andExpect(status().isOk());
		}
	}

	@Test
	void everySectionSendsAnonymousVisitorsToTheLoginPage() throws Exception {
		for (List<String> pages : PAGES_BY_ROLE.values()) {
			for (String page : pages) {
				mockMvc.perform(get(page)).andExpect(redirectedUrl("/login"));
			}
		}
	}

	@Test
	void eachRoleOpensOnlyItsOwnSection() throws Exception {
		for (String role : PAGES_BY_ROLE.keySet()) {
			for (Map.Entry<String, List<String>> section : PAGES_BY_ROLE.entrySet()) {
				for (String page : section.getValue()) {
					mockMvc.perform(get(page).with(as(role)))
							.andExpect(section.getKey().equals(role) ? status().isOk() : status().isForbidden());
				}
			}
		}
	}

	@Test
	void forbiddenPagesShowAFriendlyMessage() throws Exception {
		// MockMvc does not forward to /error like a real server does, so render it directly.
		mockMvc.perform(get("/error").with(as("APPLICANT")).accept(MediaType.TEXT_HTML)
						.requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403)
						.requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/admin"))
				.andExpect(status().isForbidden())
				.andExpect(content().string(containsString("Access denied")))
				.andExpect(content().string(containsString("Log out")));
	}

	@Test
	void postsWithoutTheCsrfTokenAreRejected() throws Exception {
		mockMvc.perform(post("/apply").with(as("APPLICANT")).param("personalInfo.firstName", "Ada"))
				.andExpect(status().isForbidden());
	}

	@Test
	void aProfessorEditsOnlyTheirOwnProfile() throws Exception {
		Long alice = users.findByEmail(ALICE).orElseThrow().getId();
		Long brian = users.findByEmail(BRIAN).orElseThrow().getId();

		mockMvc.perform(get("/professor/" + alice + "/edit").with(as("PROFESSOR"))).andExpect(status().isOk());
		mockMvc.perform(get("/professor/" + brian + "/edit").with(as("PROFESSOR"))).andExpect(status().isForbidden());
		mockMvc.perform(get("/professor/" + brian).with(as("PROFESSOR")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.not(
						containsString("href=\"/professor/" + brian + "/edit\""))));
	}

	@Test
	void afterARealLoginTheNavShowsTheUsersName() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(formLogin("/login").userParameter("email")
						.user(ALICE).password(DataSeeder.PROFESSOR_PASSWORD))
				.andReturn().getRequest().getSession();

		mockMvc.perform(get("/").session(session))
				.andExpect(content().string(containsString("Alice Chen (Professor)")))
				.andExpect(content().string(containsString("Log out")))
				.andExpect(content().string(containsString("href=\"/professor\"")));
	}
}
