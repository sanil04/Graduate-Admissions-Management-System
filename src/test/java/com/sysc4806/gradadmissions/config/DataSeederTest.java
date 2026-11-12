package com.sysc4806.gradadmissions.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.sysc4806.gradadmissions.repository.AdmissionCycleRepository;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;

/** Runs the whole app with the real seeder: no test data is created here. */
@SpringBootTest
@AutoConfigureMockMvc
class DataSeederTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private DataSeeder seeder;
	@Autowired private AdmissionCycleRepository cycles;
	@Autowired private ResearchFieldRepository fields;
	@Autowired private ProfessorRepository professors;

	@Test
	void seedingAgainDoesNotCreateASecondCycle() {
		seeder.run(null);

		assertThat(cycles.count()).isEqualTo(1);
	}

	@Test
	void validApplicationAgainstSeededDataRedirectsToTheConfirmationPage() throws Exception {
		Long fieldId = fields.findByName("AI").orElseThrow().getId();
		Long professorId = professors.findByResearchFieldsIdOrderByNameAsc(fieldId).get(0).getId();

		mockMvc.perform(post("/apply")
						.param("personalInfo.firstName", "Ada")
						.param("personalInfo.lastName", "Lovelace")
						.param("personalInfo.email", "ada@example.com")
						.param("desiredFieldId", fieldId.toString())
						.param("professorIds", professorId.toString()))
				.andExpect(redirectedUrlPattern("/apply/confirmation/*"));
	}
}
