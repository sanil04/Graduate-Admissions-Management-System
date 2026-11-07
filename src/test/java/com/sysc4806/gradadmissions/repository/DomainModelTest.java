package com.sysc4806.gradadmissions.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.sysc4806.gradadmissions.model.*;

@DataJpaTest
class DomainModelTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 12, 0);

	@Autowired private TestEntityManager em;
	@Autowired private UserRepository users;
	@Autowired private ProfessorRepository professors;
	@Autowired private ResearchFieldRepository fields;
	@Autowired private AdmissionCycleRepository cycles;
	@Autowired private ApplicationRepository applications;

	private Applicant ada;
	private AdmissionCycle cycle;
	private ResearchField ml;
	private Professor alice;

	@BeforeEach
	void setUp() {
		ada = em.persist(new Applicant("Ada Lovelace", "ada@example.com"));
		cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
		cycle.setActive(true);
		em.persist(cycle);
		ml = em.persist(new ResearchField("Machine Learning"));
		alice = em.persist(new Professor("Alice", "alice@carleton.ca"));
	}

	private Application newApplication(Applicant applicant) {
		return new Application(applicant, cycle,
				new PersonalInfo("Ada", "Lovelace", applicant.getEmail(), "613-555-0100", "Canada"));
	}

	private void flushAndClear() {
		em.flush();
		em.clear();
	}

	@Test
	void findByEmailReturnsTheRightRole() {
		em.persist(new Admin("Sam", "sam@carleton.ca"));
		flushAndClear();
		assertThat(users.findByEmail("ada@example.com")).get().isInstanceOf(Applicant.class);
		assertThat(users.findByEmail("alice@carleton.ca")).get().isInstanceOf(Professor.class);
		assertThat(users.findByEmail("sam@carleton.ca")).get().isInstanceOf(Admin.class);
		assertThat(users.findByEmail("nobody@example.com")).isEmpty();
	}

	@Test
	void emailsAreUniqueAcrossRoles() {
		assertThatThrownBy(() -> users.saveAndFlush(new Admin("Copy", "ada@example.com")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void researchFieldNamesAreUnique() {
		assertThatThrownBy(() -> fields.saveAndFlush(new ResearchField("Machine Learning")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void professorResearchFieldsAreSavedInJoinTable() {
		alice.getResearchFields().add(ml);
		flushAndClear();
		assertThat(professors.findById(alice.getId()).orElseThrow().getResearchFields())
				.containsExactly(ml);
	}

	@Test
	void findsTheActiveCycle() {
		assertThat(cycles.findFirstByActiveTrue()).get()
				.extracting(AdmissionCycle::getName).isEqualTo("Fall 2027");
	}

	@Test
	void savesAndReloadsAnApplication() {
		Application app = newApplication(ada);
		app.setDesiredField(ml);
		app.addPreferredProfessor(alice);
		Long id = applications.save(app).getId();
		flushAndClear();

		Application reloaded = applications.findById(id).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
		assertThat(reloaded.getApplicant().getEmail()).isEqualTo("ada@example.com");
		assertThat(reloaded.getCycle().getName()).isEqualTo("Fall 2027");
		assertThat(reloaded.getPersonalInfo().getCitizenship()).isEqualTo("Canada");
		assertThat(reloaded.getDesiredField()).isEqualTo(ml);
		assertThat(reloaded.getPreferredProfessors()).extracting(User::getName).containsExactly("Alice");
	}

	@Test
	void atMostThreePreferredProfessors() {
		Application app = newApplication(ada);
		for (String name : List.of("A", "B", "C")) {
			app.addPreferredProfessor(em.persist(new Professor(name, name + "@carleton.ca")));
		}
		assertThatThrownBy(() -> app.addPreferredProfessor(alice))
				.isInstanceOf(IllegalStateException.class);
		assertThat(app.getPreferredProfessors()).hasSize(3);
	}

	@Test
	void submitRules() {
		Application app = newApplication(ada);
		assertThatThrownBy(() -> app.submit(NOW)).isInstanceOf(IllegalStateException.class);

		app.setDesiredField(ml);
		app.addPreferredProfessor(alice);
		app.submit(NOW);
		assertThat(app.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
		assertThat(app.getSubmittedAt()).isEqualTo(NOW);

		assertThatThrownBy(() -> app.submit(NOW)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void listsSubmittedApplicationsOldestFirst() {
		Applicant grace = em.persist(new Applicant("Grace Hopper", "grace@example.com"));
		Application later = newApplication(grace);
		Application earlier = newApplication(ada);
		for (Application app : List.of(later, earlier)) {
			app.setDesiredField(ml);
			app.addPreferredProfessor(alice);
		}
		later.submit(NOW.plusHours(1));
		earlier.submit(NOW);
		applications.saveAll(List.of(later, earlier, newApplication(ada)));
		flushAndClear();

		assertThat(applications.findByStatusOrderBySubmittedAtAsc(ApplicationStatus.SUBMITTED))
				.extracting(a -> a.getApplicant().getEmail())
				.containsExactly("ada@example.com", "grace@example.com");
	}

	@Test
	void personalInfoIsValidatedOnSave() {
		Application app = new Application(ada, cycle, new PersonalInfo("", "L", "not-an-email", null, null));
		assertThatThrownBy(() -> applications.saveAndFlush(app))
				.isInstanceOf(ConstraintViolationException.class);
	}
}
