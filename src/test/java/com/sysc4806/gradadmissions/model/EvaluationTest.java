package com.sysc4806.gradadmissions.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The rules of {@link Evaluation#submitAssessment}, without a database. */
class EvaluationTest {

	private static final LocalDateTime ASSIGNED = LocalDateTime.of(2026, 11, 20, 9, 0);
	private static final LocalDateTime LATER = ASSIGNED.plusDays(2);

	private Evaluation evaluation;

	@BeforeEach
	void setUp() {
		AdmissionCycle cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
		Application application = new Application(new Applicant("Ada Lovelace", "ada@example.com"), cycle,
				new PersonalInfo("Ada", "Lovelace", "ada@example.com", "613-555-0100", "Canada"));
		evaluation = new Evaluation(application, new Professor("Alice", "alice@carleton.ca"), ASSIGNED);
	}

	@Test
	void aNewEvaluationIsPending() {
		assertThat(evaluation.isSubmitted()).isFalse();
		assertThat(evaluation.getRecommendation()).isNull();
		assertThat(evaluation.getSubmittedAt()).isNull();
		assertThat(evaluation.getAssignedAt()).isEqualTo(ASSIGNED);
	}

	@Test
	void submittingRecordsTheRecommendationCommentsAndTime() {
		evaluation.submitAssessment(Recommendation.RECOMMEND_WITH_FUNDING, "  Strong candidate.  ", LATER);

		assertThat(evaluation.isSubmitted()).isTrue();
		assertThat(evaluation.getRecommendation()).isEqualTo(Recommendation.RECOMMEND_WITH_FUNDING);
		assertThat(evaluation.getComments()).isEqualTo("Strong candidate.");
		assertThat(evaluation.getSubmittedAt()).isEqualTo(LATER);
	}

	@Test
	void blankCommentsAreStoredAsNull() {
		evaluation.submitAssessment(Recommendation.DO_NOT_RECOMMEND, "   ", LATER);

		assertThat(evaluation.getComments()).isNull();
	}

	@Test
	void anAssessmentCanBeSubmittedOnlyOnce() {
		evaluation.submitAssessment(Recommendation.RECOMMEND_NO_FUNDING, null, LATER);

		assertThatThrownBy(() -> evaluation.submitAssessment(Recommendation.RECOMMEND_WITH_FUNDING, null, LATER))
				.isInstanceOf(IllegalStateException.class);
		assertThat(evaluation.getRecommendation()).isEqualTo(Recommendation.RECOMMEND_NO_FUNDING);
	}

	@Test
	void aRecommendationIsRequired() {
		assertThatThrownBy(() -> evaluation.submitAssessment(null, "No choice", LATER))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(evaluation.isSubmitted()).isFalse();
	}

	@Test
	void commentsThatAreTooLongAreRejected() {
		String tooLong = "x".repeat(Evaluation.MAX_COMMENTS_LENGTH + 1);

		assertThatThrownBy(() -> evaluation.submitAssessment(Recommendation.DO_NOT_RECOMMEND, tooLong, LATER))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(evaluation.isSubmitted()).isFalse();
	}

	@Test
	void everyRecommendationHasALabelForThePages() {
		for (Recommendation recommendation : Recommendation.values()) {
			assertThat(recommendation.getLabel()).isNotBlank();
		}
	}
}
