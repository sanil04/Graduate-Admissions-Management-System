package com.sysc4806.gradadmissions.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.sysc4806.gradadmissions.model.AdmissionCycle;
import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.ApplicationStatus;
import com.sysc4806.gradadmissions.model.PersonalInfo;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;
import com.sysc4806.gradadmissions.model.User;

import jakarta.validation.ConstraintViolationException;

@DataJpaTest
class ApplicationRepositoryTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 12, 0);

	@Autowired
	private TestEntityManager em;

	@Autowired
	private ApplicationRepository applications;

	private Applicant ada;
	private AdmissionCycle cycle;
	private ResearchField ml;
	private Professor alice;
	private Professor bob;

	@BeforeEach
	void setUp() {
		ada = em.persist(new Applicant("Ada Lovelace", "ada@example.com"));
		cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
		cycle.setActive(true);
		em.persist(cycle);
		ml = em.persist(new ResearchField("Machine Learning"));
		alice = em.persist(new Professor("Alice", "alice@carleton.ca"));
		bob = em.persist(new Professor("Bob", "bob@carleton.ca"));
	}

	private Application newApplication(Applicant applicant) {
		return new Application(applicant, cycle,
				new PersonalInfo("Ada", "Lovelace", applicant.getEmail(), "613-555-0100", "Canada"));
	}

	@Test
	void savesAndReloadsAFullApplication() {
		Application app = newApplication(ada);
		app.setDesiredField(ml);
		app.addPreferredProfessor(alice);
		app.addPreferredProfessor(bob);
		Long id = applications.save(app).getId();
		em.flush();
		em.clear();

		Application reloaded = applications.findById(id).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
		assertThat(reloaded.getSubmittedAt()).isNull();
		assertThat(reloaded.getApplicant().getEmail()).isEqualTo("ada@example.com");
		assertThat(reloaded.getCycle().getName()).isEqualTo("Fall 2027");
		assertThat(reloaded.getPersonalInfo().getFullName()).isEqualTo("Ada Lovelace");
		assertThat(reloaded.getPersonalInfo().getCitizenship()).isEqualTo("Canada");
		assertThat(reloaded.getDesiredField()).isEqualTo(ml);
		assertThat(reloaded.getPreferredProfessors())
				.extracting(User::getName).containsExactlyInAnyOrder("Alice", "Bob");
	}

	@Test
	void submitSetsStatusAndTimestamp() {
		Application app = newApplication(ada);
		app.setDesiredField(ml);
		app.addPreferredProfessor(alice);
		app.submit(NOW);
		Long id = applications.save(app).getId();
		em.flush();
		em.clear();

		Application reloaded = applications.findById(id).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
		assertThat(reloaded.getSubmittedAt()).isEqualTo(NOW);
	}

	@Test
	void findByStatusListsOldestSubmissionFirst() {
		Applicant grace = em.persist(new Applicant("Grace Hopper", "grace@example.com"));
		Applicant alan = em.persist(new Applicant("Alan Turing", "alan@example.com"));

		Application later = newApplication(grace);
		later.setDesiredField(ml);
		later.addPreferredProfessor(alice);
		later.submit(NOW.plusHours(1));

		Application earlier = newApplication(ada);
		earlier.setDesiredField(ml);
		earlier.addPreferredProfessor(bob);
		earlier.submit(NOW);

		Application draft = newApplication(alan);
		applications.saveAll(java.util.List.of(later, earlier, draft));
		em.flush();
		em.clear();

		assertThat(applications.findByStatusOrderBySubmittedAtAsc(ApplicationStatus.SUBMITTED))
				.extracting(a -> a.getApplicant().getEmail())
				.containsExactly("ada@example.com", "grace@example.com");
		assertThat(applications.findByStatusOrderBySubmittedAtAsc(ApplicationStatus.DRAFT))
				.hasSize(1);
	}

	@Test
	void findByApplicant() {
		Applicant grace = em.persist(new Applicant("Grace Hopper", "grace@example.com"));
		applications.save(newApplication(ada));
		applications.save(newApplication(grace));

		assertThat(applications.findByApplicant(ada))
				.singleElement()
				.extracting(a -> a.getApplicant().getEmail()).isEqualTo("ada@example.com");
	}

	@Test
	void atMostThreePreferredProfessors() {
		Professor carol = em.persist(new Professor("Carol", "carol@carleton.ca"));
		Professor dave = em.persist(new Professor("Dave", "dave@carleton.ca"));
		Application app = newApplication(ada);
		app.addPreferredProfessor(alice);
		app.addPreferredProfessor(bob);
		app.addPreferredProfessor(carol);
		app.addPreferredProfessor(carol); // re-adding one already listed is fine

		assertThatThrownBy(() -> app.addPreferredProfessor(dave))
				.isInstanceOf(IllegalStateException.class);
		assertThat(app.getPreferredProfessors()).hasSize(3);
	}

	@Test
	void cannotSubmitWithoutFieldAndProfessor() {
		Application app = newApplication(ada);
		assertThatThrownBy(() -> app.submit(NOW)).isInstanceOf(IllegalStateException.class);

		app.setDesiredField(ml);
		assertThatThrownBy(() -> app.submit(NOW)).isInstanceOf(IllegalStateException.class);
		assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
	}

	@Test
	void cannotSubmitTwice() {
		Application app = newApplication(ada);
		app.setDesiredField(ml);
		app.addPreferredProfessor(alice);
		app.submit(NOW);

		assertThatThrownBy(() -> app.submit(NOW)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void personalInfoIsValidatedOnSave() {
		Application app = new Application(ada, cycle,
				new PersonalInfo("", "Lovelace", "not-an-email", null, null));

		assertThatThrownBy(() -> applications.saveAndFlush(app))
				.isInstanceOf(ConstraintViolationException.class);
	}
}
