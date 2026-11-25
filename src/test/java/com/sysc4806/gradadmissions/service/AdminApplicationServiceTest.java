package com.sysc4806.gradadmissions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.sysc4806.gradadmissions.model.*;

@DataJpaTest
@Import(AdminApplicationService.class)
class AdminApplicationServiceTest {

    private static final LocalDateTime NOV_7 = LocalDateTime.of(2026, 11, 7, 12, 0);

    @Autowired private TestEntityManager em;
    @Autowired private AdminApplicationService service;

    private AdmissionCycle cycle;
    private ResearchField ml;
    private Professor alice;

    @BeforeEach
    void setUp() {
        cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
        cycle.setActive(true);
        em.persist(cycle);
        ml = em.persist(new ResearchField("Machine Learning"));
        alice = new Professor("Alice Chen", "alice@carleton.ca");
        alice.getResearchFields().add(ml);
        em.persist(alice);
    }

    /** Persists an application for {@code firstName}; submitted at {@code submittedAt}, or a draft if null. */
    private Application application(String firstName, LocalDateTime submittedAt) {
        String email = firstName.toLowerCase() + "@example.com";
        Applicant applicant = em.persist(new Applicant(firstName + " Doe", email));
        Application application = new Application(applicant, cycle,
                new PersonalInfo(firstName, "Doe", email, null, null));
        application.setDesiredField(ml);
        application.addPreferredProfessor(alice);
        if (submittedAt != null) {
            application.submit(submittedAt);
        }
        return em.persist(application);
    }

    @Test
    void listsSubmittedApplicationsNewestFirst() {
        application("Older", NOV_7);
        application("Newer", NOV_7.plusDays(1));
        em.flush();
        em.clear();

        assertThat(service.submittedApplications())
                .extracting(a -> a.getPersonalInfo().getFirstName())
                .containsExactly("Newer", "Older");
    }

    @Test
    void listLeavesOutDrafts() {
        application("Submitted", NOV_7);
        application("Draft", null);

        assertThat(service.submittedApplications())
                .extracting(a -> a.getPersonalInfo().getFirstName())
                .containsExactly("Submitted");
    }

    @Test
    void listLoadsFieldAndProfessorsForTheTable() {
        application("Ada", NOV_7);
        em.flush();
        em.clear();

        Application listed = service.submittedApplications().get(0);

        // Readable without an open session: the entity graph fetched them with the list.
        em.clear();
        assertThat(listed.getDesiredField().getName()).isEqualTo("Machine Learning");
        assertThat(listed.getPreferredProfessors()).extracting(User::getName).containsExactly("Alice Chen");
    }

    @Test
    void eachFilterAndTheirCombinationSelectOnlyMatchingSubmittedApplications() {
        Application match = application("Match", NOV_7);
        Application other = application("Other", NOV_7.plusDays(1));
        other.setDesiredField(em.persist(new ResearchField("Networks")));
        other.removePreferredProfessor(alice);
        other.addPreferredProfessor(professor("Brian"));
        other.screenOut();
        application("Draft", null);
        em.flush();

        assertThat(service.submittedApplications(ml.getId(), null, null, null, null)).containsExactly(match);
        assertThat(service.submittedApplications(null, alice.getId(), null, null, null)).containsExactly(match);
        assertThat(service.submittedApplications(null, null, ApplicationStatus.SUBMITTED, null, null)).containsExactly(match);
        assertThat(service.submittedApplications(null, null, null, NOV_7.toLocalDate(), NOV_7.toLocalDate()))
                .containsExactly(match);
        assertThat(service.submittedApplications(null, null, null, NOV_7.plusDays(1).toLocalDate(), null))
                .containsExactly(other);
        assertThat(service.submittedApplications(ml.getId(), alice.getId(), ApplicationStatus.SUBMITTED,
                NOV_7.toLocalDate(), NOV_7.toLocalDate())).containsExactly(match);
        assertThat(service.submittedApplications(ml.getId(), null, ApplicationStatus.SCREENED_OUT, null, null)).isEmpty();
        assertThat(service.submittedApplications(null, null, ApplicationStatus.DRAFT, null, null)).isEmpty();
    }

    @Test
    void dateRangeIncludesTheWholeEndDateAndRejectsReversedDates() {
        Application late = application("Late", NOV_7.withHour(23).withMinute(59).withSecond(59));
        application("Tomorrow", NOV_7.plusDays(1).toLocalDate().atStartOfDay());
        assertThat(service.submittedApplications(null, null, null, null, NOV_7.toLocalDate())).containsExactly(late);
        assertThatThrownBy(() -> service.submittedApplications(null, null, null,
                NOV_7.toLocalDate(), NOV_7.minusDays(1).toLocalDate())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multiplePreferredProfessorsDoNotDuplicateAnApplicationOrHideOtherPreferences() {
        Application match = application("Match", NOV_7);
        match.addPreferredProfessor(professor("Brian"));
        em.flush();
        em.clear();
        assertThat(service.submittedApplications()).hasSize(1);
        Application filtered = service.submittedApplications(null, alice.getId(), null, null, null).getFirst();
        em.clear();
        assertThat(filtered.getPreferredProfessors()).hasSize(2);
    }

    @Test
    void findsASubmittedApplicationById() {
        Long id = application("Ada", NOV_7).getId();

        assertThat(service.findSubmittedApplication(id))
                .hasValueSatisfying(a -> assertThat(a.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED));
    }

    @Test
    void doesNotFindDraftsOrUnknownIds() {
        Long draftId = application("Draft", null).getId();

        assertThat(service.findSubmittedApplication(draftId)).isEmpty();
        assertThat(service.findSubmittedApplication(9999L)).isEmpty();
    }

    // Routing and screening out (#39)

    private static final LocalDateTime ROUTED = NOV_7.plusDays(3);

    private Professor professor(String name) {
        return em.persist(new Professor(name, name.toLowerCase().replace(' ', '.') + "@carleton.ca"));
    }

    private Application reload(Long id) {
        em.flush();
        em.clear();
        return em.find(Application.class, id);
    }

    @Test
    void routingCreatesAPendingEvaluationPerProfessorAndPutsTheApplicationUnderReview() {
        Long id = application("Ada", NOV_7).getId();
        Professor brian = professor("Brian Okonkwo");

        List<Evaluation> created = service.route(id, List.of(alice.getId(), brian.getId()), ROUTED);

        assertThat(created).extracting(e -> e.getProfessor().getName()).containsExactly("Alice Chen", "Brian Okonkwo");
        assertThat(created).allSatisfy(e -> {
            assertThat(e.isSubmitted()).isFalse();
            assertThat(e.getAssignedAt()).isEqualTo(ROUTED);
        });
        assertThat(reload(id).getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(service.evaluationsFor(id)).hasSize(2);
    }

    @Test
    void routingIgnoresTheSameProfessorListedTwice() {
        Long id = application("Ada", NOV_7).getId();

        assertThat(service.route(id, List.of(alice.getId(), alice.getId()), ROUTED)).hasSize(1);
    }

    @Test
    void anApplicationUnderReviewCanBeRoutedToMoreProfessors() {
        Long id = application("Ada", NOV_7).getId();
        Professor brian = professor("Brian Okonkwo");
        service.route(id, List.of(alice.getId()), ROUTED);
        em.flush();

        service.route(id, List.of(brian.getId()), ROUTED.plusHours(1));

        assertThat(service.evaluationsFor(id)).extracting(e -> e.getProfessor().getName())
                .containsExactly("Alice Chen", "Brian Okonkwo");
    }

    @Test
    void routingToAProfessorTwiceIsRejectedAndCreatesNothing() {
        Long id = application("Ada", NOV_7).getId();
        Professor brian = professor("Brian Okonkwo");
        service.route(id, List.of(alice.getId()), ROUTED);
        em.flush();

        assertThatThrownBy(() -> service.route(id, List.of(brian.getId(), alice.getId()), ROUTED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Alice Chen");
        assertThat(service.evaluationsFor(id)).hasSize(1);
    }

    @Test
    void routingNeedsAtLeastOneKnownProfessor() {
        Long id = application("Ada", NOV_7).getId();

        assertThatThrownBy(() -> service.route(id, List.of(), ROUTED)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.route(id, null, ROUTED)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.route(id, List.of(9999L), ROUTED)).isInstanceOf(IllegalArgumentException.class);
        assertThat(reload(id).getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    void draftsAndUnknownApplicationsCannotBeRouted() {
        Long draftId = application("Draft", null).getId();

        assertThatThrownBy(() -> service.route(draftId, List.of(alice.getId()), ROUTED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.route(9999L, List.of(alice.getId()), ROUTED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.evaluationsFor(draftId)).isEmpty();
    }

    @Test
    void screeningOutASubmittedApplication() {
        Long id = application("Ada", NOV_7).getId();

        service.screenOut(id);

        assertThat(reload(id).getStatus()).isEqualTo(ApplicationStatus.SCREENED_OUT);
    }

    @Test
    void aScreenedOutApplicationCannotBeRouted() {
        Long id = application("Ada", NOV_7).getId();
        service.screenOut(id);

        assertThatThrownBy(() -> service.route(id, List.of(alice.getId()), ROUTED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(service.evaluationsFor(id)).isEmpty();
    }

    @Test
    void aRoutedApplicationCannotBeScreenedOut() {
        Long id = application("Ada", NOV_7).getId();
        service.route(id, List.of(alice.getId()), ROUTED);

        assertThatThrownBy(() -> service.screenOut(id)).isInstanceOf(IllegalStateException.class);
        assertThat(reload(id).getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void allProfessorsAreSortedByName() {
        professor("Zoe Adams");
        professor("Brian Okonkwo");

        assertThat(service.allProfessors()).extracting(User::getName)
                .containsExactly("Alice Chen", "Brian Okonkwo", "Zoe Adams");
    }
}
