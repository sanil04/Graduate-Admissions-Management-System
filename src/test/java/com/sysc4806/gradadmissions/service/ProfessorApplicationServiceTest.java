package com.sysc4806.gradadmissions.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
}