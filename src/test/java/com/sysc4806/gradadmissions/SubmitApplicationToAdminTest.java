package com.sysc4806.gradadmissions;

import static org.hamcrest.Matchers.containsString;
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

import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;

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

	@Test
	void submittedApplicationAppearsInAdminList() throws Exception {
		Long fieldId = fields.findByName("AI").orElseThrow().getId();
		Professor professor = professors.findByResearchFieldsIdOrderByNameAsc(fieldId).get(0);

		// A name no other test submits, so the admin list check can only match this application.
		mockMvc.perform(post("/apply")
						.param("personalInfo.firstName", "Grace")
						.param("personalInfo.lastName", "Hopper")
						.param("personalInfo.email", "grace.e2e@example.com")
						.param("personalInfo.phoneNumber", "613-555-0100")
						.param("personalInfo.citizenship", "Canada")
						.param("desiredFieldId", fieldId.toString())
						.param("professorIds", professor.getId().toString()))
				.andExpect(redirectedUrlPattern("/apply/confirmation/*"));

		mockMvc.perform(get("/admin/applications"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Grace Hopper")))
				.andExpect(content().string(containsString("AI")))
				.andExpect(content().string(containsString(professor.getName())))
				.andExpect(content().string(containsString("SUBMITTED")));
	}
}
