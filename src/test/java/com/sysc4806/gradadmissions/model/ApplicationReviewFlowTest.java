package com.sysc4806.gradadmissions.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The admin's status changes on {@link Application}: routing to professors and screening out. */
class ApplicationReviewFlowTest {

	private static final LocalDateTime NOV_7 = LocalDateTime.of(2026, 11, 7, 12, 0);

	private Application application;

	@BeforeEach
	void setUp() {
		AdmissionCycle cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
		application = new Application(new Applicant("Ada Lovelace", "ada@example.com"), cycle,
				new PersonalInfo("Ada", "Lovelace", "ada@example.com", null, null));
		application.setDesiredField(new ResearchField("AI"));
		application.addPreferredProfessor(new Professor("Alice", "alice@carleton.ca"));
	}

	@Test
	void aDraftCanNeitherBeRoutedNorScreenedOut() {
		assertThat(application.isRoutable()).isFalse();
		assertThat(application.isScreenable()).isFalse();
		assertThatThrownBy(application::startReview).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(application::screenOut).isInstanceOf(IllegalStateException.class);
		assertThat(application.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
	}

	@Test
	void routingASubmittedApplicationPutsItUnderReview() {
		application.submit(NOV_7);

		application.startReview();

		assertThat(application.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
	}

	@Test
	void anApplicationUnderReviewCanBeRoutedAgainButNotScreenedOut() {
		application.submit(NOV_7);
		application.startReview();

		assertThat(application.isRoutable()).isTrue();
		application.startReview();
		assertThat(application.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);

		assertThat(application.isScreenable()).isFalse();
		assertThatThrownBy(application::screenOut).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void aSubmittedApplicationCanBeScreenedOut() {
		application.submit(NOV_7);

		application.screenOut();

		assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SCREENED_OUT);
	}

	@Test
	void aScreenedOutApplicationCanNoLongerBeRouted() {
		application.submit(NOV_7);
		application.screenOut();

		assertThat(application.isRoutable()).isFalse();
		assertThatThrownBy(application::startReview).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(application::screenOut).isInstanceOf(IllegalStateException.class);
	}
}
