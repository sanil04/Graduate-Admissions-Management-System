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
import org.springframework.dao.DataIntegrityViolationException;

import com.sysc4806.gradadmissions.model.*;

@DataJpaTest
class EvaluationRepositoryTest {

	private static final LocalDateTime ROUTED = LocalDateTime.of(2026, 11, 20, 9, 0);

	@Autowired private TestEntityManager em;
	@Autowired private EvaluationRepository evaluations;

	private Application adasApplication;
	private Application gracesApplication;
	private Professor alice;
	private Professor brian;

	@BeforeEach
	void setUp() {
		AdmissionCycle cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
		cycle.setActive(true);
		em.persist(cycle);
		alice = em.persist(new Professor("Alice", "alice@carleton.ca"));
		brian = em.persist(new Professor("Brian", "brian@carleton.ca"));
		adasApplication = em.persist(application(cycle, "Ada", "ada@example.com"));
		gracesApplication = em.persist(application(cycle, "Grace", "grace@example.com"));
	}

	private Application application(AdmissionCycle cycle, String firstName, String email) {
		Applicant applicant = em.persist(new Applicant(firstName, email));
		return new Application(applicant, cycle, new PersonalInfo(firstName, "Test", email, "613-555-0100", "Canada"));
	}

	private void flushAndClear() {
		em.flush();
		em.clear();
	}

	@Test
	void aPendingEvaluationIsSavedWithoutARecommendation() {
		Long id = evaluations.save(new Evaluation(adasApplication, alice, ROUTED)).getId();
		flushAndClear();

		Evaluation saved = evaluations.findById(id).orElseThrow();
		assertThat(saved.isSubmitted()).isFalse();
		assertThat(saved.getApplication().getId()).isEqualTo(adasApplication.getId());
		assertThat(saved.getProfessor().getId()).isEqualTo(alice.getId());
		assertThat(saved.getAssignedAt()).isEqualTo(ROUTED);
	}

	@Test
	void aSubmittedAssessmentIsSaved() {
		Evaluation evaluation = evaluations.save(new Evaluation(adasApplication, alice, ROUTED));
		evaluation.submitAssessment(Recommendation.RECOMMEND_NO_SUPERVISION, "Good fit for Brian's lab", ROUTED.plusDays(1));
		flushAndClear();

		Evaluation saved = evaluations.findById(evaluation.getId()).orElseThrow();
		assertThat(saved.getRecommendation()).isEqualTo(Recommendation.RECOMMEND_NO_SUPERVISION);
		assertThat(saved.getComments()).isEqualTo("Good fit for Brian's lab");
		assertThat(saved.getSubmittedAt()).isEqualTo(ROUTED.plusDays(1));
	}

	@Test
	void anApplicationCanBeRoutedToTheSameProfessorOnlyOnce() {
		evaluations.saveAndFlush(new Evaluation(adasApplication, alice, ROUTED));

		assertThatThrownBy(() -> evaluations.saveAndFlush(new Evaluation(adasApplication, alice, ROUTED.plusHours(1))))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findsAProfessorsAssignedApplicationsNewestFirst() {
		evaluations.save(new Evaluation(adasApplication, alice, ROUTED));
		evaluations.save(new Evaluation(gracesApplication, alice, ROUTED.plusHours(2)));
		evaluations.save(new Evaluation(gracesApplication, brian, ROUTED.plusHours(3)));
		flushAndClear();

		assertThat(evaluations.findByProfessorIdOrderByAssignedAtDesc(alice.getId()))
				.extracting(e -> e.getApplication().getId())
				.containsExactly(gracesApplication.getId(), adasApplication.getId());
	}

	@Test
	void findsEveryProfessorAnApplicationWasRoutedTo() {
		evaluations.save(new Evaluation(gracesApplication, brian, ROUTED.plusHours(1)));
		evaluations.save(new Evaluation(gracesApplication, alice, ROUTED));
		evaluations.save(new Evaluation(adasApplication, alice, ROUTED));
		flushAndClear();

		assertThat(evaluations.findByApplicationIdOrderByAssignedAtAsc(gracesApplication.getId()))
				.extracting(e -> e.getProfessor().getName())
				.containsExactly("Alice", "Brian");
	}

	@Test
	void findsTheEvaluationOfOneApplicationByOneProfessor() {
		evaluations.save(new Evaluation(adasApplication, alice, ROUTED));
		flushAndClear();

		assertThat(evaluations.findByApplicationIdAndProfessorId(adasApplication.getId(), alice.getId())).isPresent();
		assertThat(evaluations.findByApplicationIdAndProfessorId(adasApplication.getId(), brian.getId())).isEmpty();
		assertThat(evaluations.existsByApplicationIdAndProfessorId(adasApplication.getId(), alice.getId())).isTrue();
		assertThat(evaluations.existsByApplicationIdAndProfessorId(gracesApplication.getId(), alice.getId())).isFalse();
	}
}
