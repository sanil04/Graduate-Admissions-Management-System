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
}
