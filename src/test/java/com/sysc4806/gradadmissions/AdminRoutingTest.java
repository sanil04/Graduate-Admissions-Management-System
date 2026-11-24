package com.sysc4806.gradadmissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.sysc4806.gradadmissions.config.DataSeeder;
import com.sysc4806.gradadmissions.model.*;
import com.sysc4806.gradadmissions.repository.*;

/** The admin routes and screens out applications in the whole app, with the seeded data and security (#39). */
@SpringBootTest
@AutoConfigureMockMvc
class AdminRoutingTest {

	private static final RequestPostProcessor ADMIN = user(DataSeeder.ADMIN_EMAIL).roles("ADMIN");

	@Autowired private MockMvc mockMvc;
	@Autowired private ApplicantRepository applicants;
	@Autowired private ApplicationRepository applications;
	@Autowired private AdmissionCycleRepository cycles;
	@Autowired private ResearchFieldRepository fields;
	@Autowired private ProfessorRepository professors;
	@Autowired private EvaluationRepository evaluations;
	@Autowired private UserRepository users;

	private Professor seededProfessor(String email) {
		return (Professor) users.findByEmail(email).orElseThrow();
	}

	/** A submitted application in the seeded cycle, preferring Alice Chen. The email keeps it apart from other tests. */
	private Application submitted(String firstName) {
		String email = firstName.toLowerCase() + ".routing@example.com";
		Applicant applicant = applicants.save(new Applicant(firstName + " Routing", email));
		Application application = new Application(applicant, cycles.findByActiveTrue().orElseThrow(),
				new PersonalInfo(firstName, "Routing", email, null, "Canada"));
		application.setDesiredField(fields.findByName("AI").orElseThrow());
		application.addPreferredProfessor(seededProfessor("achen@gradadmissions.local"));
		application.submit(LocalDateTime.now());
		return applications.save(application);
	}

	@Test
	void theAdminRoutesAnApplicationAndSeesItAwaitingTheProfessor() throws Exception {
		Long id = submitted("Katherine").getId();
		Professor alice = seededProfessor("achen@gradadmissions.local");
		Professor brian = seededProfessor("bokonkwo@gradadmissions.local");

		mockMvc.perform(post("/admin/applications/" + id + "/route").with(ADMIN).with(csrf())
						.param("professorIds", alice.getId().toString(), brian.getId().toString()))
				.andExpect(redirectedUrl("/admin/applications/" + id));

		assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
		assertThat(evaluations.findByApplicationIdOrderByAssignedAtAsc(id))
				.extracting(e -> e.getProfessor().getEmail())
				.containsExactlyInAnyOrder("achen@gradadmissions.local", "bokonkwo@gradadmissions.local");

		mockMvc.perform(get("/admin/applications/" + id).with(ADMIN))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("UNDER_REVIEW")))
				.andExpect(content().string(containsString("Alice Chen")))
				.andExpect(content().string(containsString("Brian Okonkwo")))
				.andExpect(content().string(containsString("Pending")));
	}

	@Test
	void theAdminScreensOutAnApplication() throws Exception {
		Long id = submitted("Dorothy").getId();

		mockMvc.perform(post("/admin/applications/" + id + "/screen-out").with(ADMIN).with(csrf()))
				.andExpect(redirectedUrl("/admin/applications/" + id));

		assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.SCREENED_OUT);
		assertThat(evaluations.findByApplicationIdOrderByAssignedAtAsc(id)).isEmpty();
	}

	@Test
	void aProfessorCannotRouteApplications() throws Exception {
		Long id = submitted("Mary").getId();
		Professor alice = seededProfessor("achen@gradadmissions.local");

		mockMvc.perform(post("/admin/applications/" + id + "/route")
						.with(user("achen@gradadmissions.local").roles("PROFESSOR")).with(csrf())
						.param("professorIds", alice.getId().toString()))
				.andExpect(status().isForbidden());

		assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
	}
}
