package com.sysc4806.gradadmissions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.sysc4806.gradadmissions.config.DataSeeder;
import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.repository.AdmissionCycleRepository;
import com.sysc4806.gradadmissions.repository.ApplicationRepository;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;

/**
 * End to end: submit an application through the applicant form, then confirm
 * it shows up on the admin list (issue 14).
 * The class name must end in "Test": Maven does not run classes ending in "IT".
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubmitApplicationToAdminTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private ResearchFieldRepository fields;
	@Autowired private ProfessorRepository professors;
	@Autowired private UserRepository users;
	@Autowired private AdmissionCycleRepository cycles;
	@Autowired private ApplicationRepository applications;

	@Test
	void submittedApplicationAppearsInAdminList() throws Exception {
		Long fieldId = fields.findByName("AI").orElseThrow().getId();
		Professor professor = professors.findByResearchFieldsIdOrderByNameAsc(fieldId).get(0);

		// Other SpringBootTests may already have submitted for the seeded applicant.
		Applicant applicant = (Applicant) users.findByEmail(DataSeeder.APPLICANT_EMAIL).orElseThrow();
		cycles.findByActiveTrue().ifPresent(cycle ->
				applications.findByApplicantIdAndCycleId(applicant.getId(), cycle.getId())
						.ifPresent(applications::delete));

		// Distinct personal name so the admin list check can only match this submission.
		mockMvc.perform(post("/apply")
						.with(user(DataSeeder.APPLICANT_EMAIL).roles("APPLICANT"))
						.with(csrf())
						.param("personalInfo.firstName", "Grace")
						.param("personalInfo.lastName", "Hopper")
						.param("personalInfo.email", DataSeeder.APPLICANT_EMAIL)
						.param("personalInfo.phoneNumber", "613-555-0100")
						.param("personalInfo.citizenship", "Canada")
						.param("desiredFieldId", fieldId.toString())
						.param("professorIds", professor.getId().toString()))
				.andExpect(redirectedUrlPattern("/apply/confirmation/*"));

		mockMvc.perform(get("/admin/applications").with(user(DataSeeder.ADMIN_EMAIL).roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Grace Hopper")))
				.andExpect(content().string(containsString("AI")))
				.andExpect(content().string(containsString(professor.getName())))
				.andExpect(content().string(containsString("SUBMITTED")));
	}
}
