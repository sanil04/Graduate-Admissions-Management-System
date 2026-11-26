package com.sysc4806.gradadmissions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.sysc4806.gradadmissions.config.DataSeeder;
import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.Recommendation;
import com.sysc4806.gradadmissions.repository.AdmissionCycleRepository;
import com.sysc4806.gradadmissions.repository.ApplicationRepository;
import com.sysc4806.gradadmissions.repository.DocumentRepository;
import com.sysc4806.gradadmissions.repository.EvaluationRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;

/**
 * End-to-end test: applicant submits, admin routes, professor assesses, admin sees the
 * recommendation (issue 46).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApplyRouteEvaluateE2ETest {

	private static final String ALICE = "achen@gradadmissions.local";

	@Autowired private MockMvc mockMvc;
	@Autowired private ResearchFieldRepository fields;
	@Autowired private UserRepository users;
	@Autowired private AdmissionCycleRepository cycles;
	@Autowired private ApplicationRepository applications;
	@Autowired private EvaluationRepository evaluations;
	@Autowired private DocumentRepository documents;

	@Test
	void applyRouteEvaluateThenAdminSeesTheRecommendation() throws Exception {
		Long fieldId = fields.findByName("AI").orElseThrow().getId();
		Professor alice = (Professor) users.findByEmail(ALICE).orElseThrow();

		// Shared @SpringBootTest DB: wipe evaluations/documents before deleting the application.
		clearSeededApplicantApplication();

		MvcResult submitted = mockMvc.perform(post("/apply")
						.with(user(DataSeeder.APPLICANT_EMAIL).roles("APPLICANT"))
						.with(csrf())
						.param("personalInfo.firstName", "Edsger")
						.param("personalInfo.lastName", "Dijkstra")
						.param("personalInfo.email", DataSeeder.APPLICANT_EMAIL)
						.param("personalInfo.phoneNumber", "613-555-0199")
						.param("personalInfo.citizenship", "Netherlands")
						.param("desiredFieldId", fieldId.toString())
						.param("professorIds", alice.getId().toString()))
				.andExpect(redirectedUrlPattern("/apply/confirmation/*"))
				.andReturn();

		String confirmationUrl = submitted.getResponse().getRedirectedUrl();
		Long applicationId = Long.valueOf(confirmationUrl.substring(confirmationUrl.lastIndexOf('/') + 1));

		mockMvc.perform(post("/admin/applications/" + applicationId + "/route")
						.with(user(DataSeeder.ADMIN_EMAIL).roles("ADMIN"))
						.with(csrf())
						.param("professorIds", alice.getId().toString()))
				.andExpect(redirectedUrl("/admin/applications/" + applicationId));

		mockMvc.perform(post("/professor/applications/" + applicationId + "/assessment")
						.with(user(ALICE).roles("PROFESSOR"))
						.with(csrf())
						.param("recommendation", Recommendation.RECOMMEND_WITH_FUNDING.name())
						.param("comments", "Strong algorithms background."))
				.andExpect(redirectedUrl("/professor/applications/" + applicationId));

		mockMvc.perform(get("/admin/applications/" + applicationId)
						.with(user(DataSeeder.ADMIN_EMAIL).roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Alice Chen")))
				.andExpect(content().string(containsString(Recommendation.RECOMMEND_WITH_FUNDING.getLabel())))
				.andExpect(content().string(containsString("Strong algorithms background.")))
				.andExpect(content().string(not(containsString(">Pending</span>"))));

		// Don't leave an assessed application for later SpringBootTests to trip over.
		clearSeededApplicantApplication();
	}

	/** Deletes the seeded applicant's active-cycle application and its child rows first. */
	private void clearSeededApplicantApplication() {
		Applicant applicant = (Applicant) users.findByEmail(DataSeeder.APPLICANT_EMAIL).orElseThrow();
		cycles.findByActiveTrue().ifPresent(cycle ->
				applications.findByApplicantIdAndCycleId(applicant.getId(), cycle.getId())
						.ifPresent(this::deleteApplicationAndChildren));
	}

	private void deleteApplicationAndChildren(Application app) {
		evaluations.findByApplicationIdOrderByAssignedAtAsc(app.getId()).forEach(evaluations::delete);
		documents.findByApplicationIdOrderByTypeAsc(app.getId()).forEach(documents::delete);
		applications.delete(app);
	}
}