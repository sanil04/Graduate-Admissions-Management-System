package com.sysc4806.gradadmissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
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

import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.repository.ApplicationRepository;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;

/**
 * Issue #37 end to end, with the real login form, security rules and seeded data: a new applicant
 * registers, logs in, applies, and is the only applicant who can open the confirmation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RegistrationFlowTest {

	private static final String PASSWORD = "secret123";

	@Autowired private MockMvc mockMvc;
	@Autowired private ResearchFieldRepository fields;
	@Autowired private ProfessorRepository professors;
	@Autowired private ApplicationRepository applications;

	private void register(String firstName, String lastName, String email) throws Exception {
		mockMvc.perform(post("/register").with(csrf())
						.param("firstName", firstName)
						.param("lastName", lastName)
						.param("email", email)
						.param("password", PASSWORD)
						.param("confirmPassword", PASSWORD))
				.andExpect(redirectedUrl("/login?registered"));
	}

	private MockHttpSession logIn(String email) throws Exception {
		return (MockHttpSession) mockMvc.perform(formLogin("/login").userParameter("email")
						.user(email).password(PASSWORD))
				.andExpect(redirectedUrl("/applicant"))
				.andExpect(authenticated().withRoles("APPLICANT"))
				.andReturn().getRequest().getSession(false);
	}

	@Test
	void aNewApplicantRegistersLogsInAndApplies() throws Exception {
		// Emails no other test uses: the application context, and so the database, is shared.
		register("Grace", "Hopper", "Grace.Flow@Example.com");
		// Stored in lower case; logging in works whatever the case.
		MockHttpSession grace = logIn("grace.flow@example.com");

		mockMvc.perform(get("/apply").session(grace))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("value=\"Grace\"")))
				.andExpect(content().string(containsString("value=\"Hopper\"")))
				.andExpect(content().string(containsString("value=\"grace.flow@example.com\"")));

		Long fieldId = fields.findByName("AI").orElseThrow().getId();
		Long professorId = professors.findByResearchFieldsIdOrderByNameAsc(fieldId).get(0).getId();
		String confirmation = mockMvc.perform(post("/apply").session(grace).with(csrf())
						.param("personalInfo.firstName", "Grace")
						.param("personalInfo.lastName", "Hopper")
						.param("personalInfo.email", "someone.else@example.com")
						.param("desiredFieldId", fieldId.toString())
						.param("professorIds", professorId.toString()))
				.andExpect(redirectedUrlPattern("/apply/confirmation/*"))
				.andReturn().getResponse().getRedirectedUrl();

		mockMvc.perform(get(confirmation).session(grace))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Thank you, <span>Grace</span>")));

		Long id = Long.valueOf(confirmation.substring(confirmation.lastIndexOf('/') + 1));
		Application saved = applications.findById(id).orElseThrow();
		assertThat(saved.getPersonalInfo().getEmail()).isEqualTo("grace.flow@example.com");

		// Another applicant can't open it, even knowing the id.
		register("Mallory", "Evil", "mallory.flow@example.com");
		mockMvc.perform(get(confirmation).session(logIn("mallory.flow@example.com")))
				.andExpect(status().isNotFound());
	}

	@Test
	void anEmailCanOnlyBeRegisteredOnce() throws Exception {
		register("Ada", "Lovelace", "ada.flow@example.com");

		mockMvc.perform(post("/register").with(csrf())
						.param("firstName", "Ada")
						.param("lastName", "Again")
						.param("email", "ADA.flow@example.com")
						.param("password", PASSWORD)
						.param("confirmPassword", PASSWORD))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("already exists")));
	}
}
