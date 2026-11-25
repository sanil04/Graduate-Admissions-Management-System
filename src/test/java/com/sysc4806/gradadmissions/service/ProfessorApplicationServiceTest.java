package com.sysc4806.gradadmissions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.sysc4806.gradadmissions.model.*;

@DataJpaTest
@Import(ProfessorApplicationService.class)
class ProfessorApplicationServiceTest {

    private static final LocalDateTime NOV_7 = LocalDateTime.of(2026, 11, 7, 12, 0);
    private static final LocalDateTime NOV_20 = LocalDateTime.of(2026, 11, 20, 9, 0);

    @Autowired private TestEntityManager em;
    @Autowired private ProfessorApplicationService service;

    private AdmissionCycle cycle;
    private ResearchField ml;
    private Professor alice;
    private Professor bob;

    @BeforeEach
    void setUp() {
        cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
        cycle.setActive(true);
        em.persist(cycle);
        ml = em.persist(new ResearchField("Machine Learning"));
        alice = persistProfessor("Alice Chen", "alice@carleton.ca");
        bob = persistProfessor("Bob Stone", "bob@carleton.ca");
    }

    private Professor persistProfessor(String name, String email) {
        Professor professor = new Professor(name, email);
        professor.getResearchFields().add(ml);
        return em.persist(professor);
    }

    private Application submittedApplication(String firstName) {
        String email = firstName.toLowerCase() + "@example.com";
        Applicant applicant = em.persist(new Applicant(firstName + " Doe", email));
        Application application = new Application(applicant, cycle,
                new PersonalInfo(firstName, "Doe", email, null, null));
        application.setDesiredField(ml);
        application.addPreferredProfessor(alice);
        application.submit(NOV_7);
        return em.persist(application);
    }

    private Evaluation route(Application application, Professor professor, LocalDateTime at) {
        return em.persist(new Evaluation(application, professor, at));
    }

    @Test
    void assignedToListsOnlyTheProfessorsOwnApplicationsNewestFirst() {
        Application older = submittedApplication("Older");
        Application newer = submittedApplication("Newer");
        Application forBob = submittedApplication("Other");
        route(older, alice, NOV_20);
        route(newer, alice, NOV_20.plusDays(1));
        route(forBob, bob, NOV_20);
        em.flush();
        em.clear();

        assertThat(service.assignedTo(alice.getId()))
                .extracting(evaluation -> evaluation.getApplication().getPersonalInfo().getFirstName())
                .containsExactly("Newer", "Older");
    }

    @Test
    void assignedToIsEmptyWhenNothingWasRoutedToTheProfessor() {
        route(submittedApplication("Ada"), bob, NOV_20);
        em.flush();
        em.clear();

        assertThat(service.assignedTo(alice.getId())).isEmpty();
    }

    @Test
    void assignedToShowsPendingAndSubmittedAssessments() {
        Evaluation pending = route(submittedApplication("Pending"), alice, NOV_20);
        Evaluation done = route(submittedApplication("Done"), alice, NOV_20.plusDays(1));
        done.submitAssessment(Recommendation.RECOMMEND_WITH_FUNDING, "Strong", NOV_20.plusDays(2));
        em.flush();
        em.clear();

        assertThat(service.assignedTo(alice.getId()))
                .extracting(Evaluation::getId, Evaluation::isSubmitted)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(done.getId(), true),
                        org.assertj.core.groups.Tuple.tuple(pending.getId(), false));
    }

    @Test
    void assignedToLoadsWhatTheListPageShows() {
        route(submittedApplication("Ada"), alice, NOV_20);
        em.flush();
        em.clear();

        Evaluation evaluation = service.assignedTo(alice.getId()).get(0);

        assertThat(evaluation.getApplication().getDesiredField().getName()).isEqualTo("Machine Learning");
        assertThat(evaluation.getAssignedAt()).isEqualTo(NOV_20);
    }

    @Test
    void findAssignedReturnsTheEvaluationForARoutedApplication() {
        Application application = submittedApplication("Ada");
        Evaluation evaluation = route(application, alice, NOV_20);
        em.flush();
        em.clear();

        assertThat(service.findAssigned(application.getId(), alice.getId()))
                .get().extracting(Evaluation::getId).isEqualTo(evaluation.getId());
    }

    @Test
    void findAssignedIsEmptyForAnApplicationRoutedToAnotherProfessor() {
        Application application = submittedApplication("Ada");
        route(application, bob, NOV_20);
        em.flush();
        em.clear();

        assertThat(service.findAssigned(application.getId(), alice.getId())).isEmpty();
    }

    @Test
    void findAssignedIsEmptyForAnApplicationThatWasNeverRouted() {
        Application application = submittedApplication("Ada");
        em.flush();
        em.clear();

        assertThat(service.findAssigned(application.getId(), alice.getId())).isEmpty();
    }

    @Test
    void findAssignedIsEmptyForAnUnknownApplication() {
        assertThat(service.findAssigned(999L, alice.getId())).isEmpty();
    }

    // Submitting an assessment (#42)

    private Evaluation reload(Evaluation evaluation) {
        em.flush();
        em.clear();
        return em.find(Evaluation.class, evaluation.getId());
    }

    @Test
    void submitAssessmentSavesTheRecommendationAndComments() {
        Application application = submittedApplication("Ada");
        Evaluation evaluation = route(application, alice, NOV_20);
        em.flush();
        em.clear();
        LocalDateTime now = NOV_20.plusDays(1);

        service.submitAssessment(application.getId(), alice.getId(),
                Recommendation.RECOMMEND_WITH_FUNDING, "  Strong candidate.  ", now);

        Evaluation saved = reload(evaluation);
        assertThat(saved.isSubmitted()).isTrue();
        assertThat(saved.getRecommendation()).isEqualTo(Recommendation.RECOMMEND_WITH_FUNDING);
        assertThat(saved.getComments()).isEqualTo("Strong candidate.");
        assertThat(saved.getSubmittedAt()).isEqualTo(now);
    }

    @Test
    void submitAssessmentStoresBlankCommentsAsNull() {
        Application application = submittedApplication("Ada");
        Evaluation evaluation = route(application, alice, NOV_20);
        em.flush();
        em.clear();

        service.submitAssessment(application.getId(), alice.getId(), Recommendation.DO_NOT_RECOMMEND, "   ", NOV_20);

        assertThat(reload(evaluation).getComments()).isNull();
    }

    @Test
    void submitAssessmentDoesNotChangeTheApplicationStatus() {
        Application application = submittedApplication("Ada");
        application.startReview();
        route(application, alice, NOV_20);
        em.flush();
        em.clear();

        service.submitAssessment(application.getId(), alice.getId(), Recommendation.RECOMMEND_NO_FUNDING, null, NOV_20);
        em.flush();
        em.clear();

        assertThat(em.find(Application.class, application.getId()).getStatus())
                .isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void submitAssessmentCannotBeDoneTwiceAndKeepsTheFirstAssessment() {
        Application application = submittedApplication("Ada");
        Evaluation evaluation = route(application, alice, NOV_20);
        em.flush();
        em.clear();
        service.submitAssessment(application.getId(), alice.getId(), Recommendation.RECOMMEND_WITH_FUNDING, "First", NOV_20);

        assertThatThrownBy(() -> service.submitAssessment(application.getId(), alice.getId(),
                Recommendation.DO_NOT_RECOMMEND, "Second", NOV_20.plusDays(1)))
                .isInstanceOf(IllegalStateException.class);

        Evaluation saved = reload(evaluation);
        assertThat(saved.getRecommendation()).isEqualTo(Recommendation.RECOMMEND_WITH_FUNDING);
        assertThat(saved.getComments()).isEqualTo("First");
    }

    @Test
    void submitAssessmentReportsAConcurrentSubmitAsAlreadySubmitted() {
        Application application = submittedApplication("Ada");
        Evaluation evaluation = route(application, alice, NOV_20);
        em.flush();
        em.clear();
        // A second request read the evaluation as unsubmitted, but the first one has since saved it.
        Evaluation stale = em.find(Evaluation.class, evaluation.getId());
        em.getEntityManager().detach(stale);
        service.submitAssessment(application.getId(), alice.getId(), Recommendation.RECOMMEND_WITH_FUNDING, "First", NOV_20);
        em.flush();
        em.clear();

        stale.submitAssessment(Recommendation.DO_NOT_RECOMMEND, "Second", NOV_20.plusDays(1));

        assertThatThrownBy(() -> {
            em.getEntityManager().merge(stale);
            em.flush();
        }).isInstanceOf(jakarta.persistence.OptimisticLockException.class);
        em.clear();
        assertThat(reload(evaluation).getComments()).isEqualTo("First");
    }

    @Test
    void submitAssessmentRequiresARecommendation() {
        Application application = submittedApplication("Ada");
        Evaluation evaluation = route(application, alice, NOV_20);
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.submitAssessment(application.getId(), alice.getId(), null, "Comments", NOV_20))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(reload(evaluation).isSubmitted()).isFalse();
    }

    @Test
    void submitAssessmentRejectsTooLongComments() {
        Application application = submittedApplication("Ada");
        Evaluation evaluation = route(application, alice, NOV_20);
        em.flush();
        em.clear();
        String tooLong = "x".repeat(Evaluation.MAX_COMMENTS_LENGTH + 1);

        assertThatThrownBy(() -> service.submitAssessment(application.getId(), alice.getId(),
                Recommendation.RECOMMEND_NO_FUNDING, tooLong, NOV_20))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(reload(evaluation).isSubmitted()).isFalse();
    }

    @Test
    void submitAssessmentOnlyAffectsTheProfessorsOwnEvaluation() {
        Application application = submittedApplication("Ada");
        Evaluation forAlice = route(application, alice, NOV_20);
        Evaluation forBob = route(application, bob, NOV_20);
        em.flush();
        em.clear();

        service.submitAssessment(application.getId(), alice.getId(), Recommendation.RECOMMEND_WITH_FUNDING, null, NOV_20);

        assertThat(reload(forAlice).isSubmitted()).isTrue();
        assertThat(em.find(Evaluation.class, forBob.getId()).isSubmitted()).isFalse();
    }

    @Test
    void submitAssessmentFailsForAnApplicationRoutedToAnotherProfessor() {
        Application application = submittedApplication("Ada");
        Evaluation forBob = route(application, bob, NOV_20);
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.submitAssessment(application.getId(), alice.getId(),
                Recommendation.RECOMMEND_WITH_FUNDING, null, NOV_20))
                .isInstanceOf(NoSuchElementException.class);

        assertThat(reload(forBob).isSubmitted()).isFalse();
    }

    @Test
    void submitAssessmentFailsForAnUnknownApplication() {
        assertThatThrownBy(() -> service.submitAssessment(999L, alice.getId(),
                Recommendation.RECOMMEND_WITH_FUNDING, null, NOV_20))
                .isInstanceOf(NoSuchElementException.class);
    }
}