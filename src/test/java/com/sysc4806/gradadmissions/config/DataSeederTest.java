package com.sysc4806.gradadmissions.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.sysc4806.gradadmissions.model.User;
import com.sysc4806.gradadmissions.repository.AdmissionCycleRepository;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;

/** Runs the whole app with the real seeder: no test data is created here. */
@SpringBootTest
@AutoConfigureMockMvc
class DataSeederTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private DataSeeder seeder;
	@Autowired private AdmissionCycleRepository cycles;
	@Autowired private ResearchFieldRepository fields;
	@Autowired private ProfessorRepository professors;
	@Autowired private UserRepository users;
	@Autowired private PasswordEncoder passwordEncoder;

	@Test
	void seedingAgainDoesNotCreateASecondCycle() {
		seeder.run(null);

		assertThat(cycles.count()).isEqualTo(1);
	}

	@Test
	void seededUsersGetBcryptHashedPasswords() {
		User admin = users.findByEmail(DataSeeder.ADMIN_EMAIL).orElseThrow();
		User professor = users.findByEmail("achen@gradadmissions.local").orElseThrow();
		User applicant = users.findByEmail(DataSeeder.APPLICANT_EMAIL).orElseThrow();

		assertThat(admin.getPasswordHash()).startsWith("$2").isNotEqualTo(DataSeeder.ADMIN_PASSWORD);
		assertThat(passwordEncoder.matches(DataSeeder.ADMIN_PASSWORD, admin.getPasswordHash())).isTrue();
		assertThat(passwordEncoder.matches(DataSeeder.PROFESSOR_PASSWORD, professor.getPasswordHash())).isTrue();
		assertThat(passwordEncoder.matches(DataSeeder.APPLICANT_PASSWORD, applicant.getPasswordHash())).isTrue();
	}

	@Test
	void usersSeededBeforePasswordsExistedGetOneOnTheNextStart() {
		// The production database already had the seeded users, without a password column.
		User admin = users.findByEmail(DataSeeder.ADMIN_EMAIL).orElseThrow();
		admin.setPasswordHash(null);
		users.save(admin);

		seeder.run(null);

		String hash = users.findByEmail(DataSeeder.ADMIN_EMAIL).orElseThrow().getPasswordHash();
		assertThat(passwordEncoder.matches(DataSeeder.ADMIN_PASSWORD, hash)).isTrue();
	}

	@Test
	void seedingAgainDoesNotOverwriteAChangedPassword() {
		User admin = users.findByEmail(DataSeeder.ADMIN_EMAIL).orElseThrow();
		String original = admin.getPasswordHash();
		admin.setPasswordHash(passwordEncoder.encode("changed"));
		users.save(admin);
		try {
			seeder.run(null);

			String hash = users.findByEmail(DataSeeder.ADMIN_EMAIL).orElseThrow().getPasswordHash();
			assertThat(passwordEncoder.matches("changed", hash)).isTrue();
		} finally {
			admin.setPasswordHash(original);
			users.save(admin);
		}
	}

	@Test
	void validApplicationAgainstSeededDataRedirectsToTheConfirmationPage() throws Exception {
		Long fieldId = fields.findByName("AI").orElseThrow().getId();
		Long professorId = professors.findByResearchFieldsIdOrderByNameAsc(fieldId).get(0).getId();

		mockMvc.perform(post("/apply")
						.with(user(DataSeeder.APPLICANT_EMAIL).roles("APPLICANT"))
						.with(csrf())
						.param("personalInfo.firstName", "Ada")
						.param("personalInfo.lastName", "Lovelace")
						.param("desiredFieldId", fieldId.toString())
						.param("professorIds", professorId.toString()))
				.andExpect(redirectedUrlPattern("/apply/confirmation/*"));
	}
}
