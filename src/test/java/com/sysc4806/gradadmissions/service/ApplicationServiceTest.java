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
import com.sysc4806.gradadmissions.repository.ApplicationRepository;

@DataJpaTest
@Import(ApplicationService.class)
class ApplicationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 12, 0);

    @Autowired private TestEntityManager em;
    @Autowired private ApplicationService service;
    @Autowired private ApplicationRepository applications;

    private Applicant ada;
    private AdmissionCycle cycle;
    private ResearchField ml;
    private Professor alice;
    private PersonalInfo info;

    @BeforeEach
    void setUp() {
        ada = em.persist(new Applicant("Ada Lovelace", "ada@example.com"));
        cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
        cycle.setActive(true);
        em.persist(cycle);
        ml = em.persist(new ResearchField("Machine Learning"));
        alice = professorIn("Alice", ml);
        info = new PersonalInfo("Ada", "Lovelace", "ada@example.com", "613-555-0100", "Canada");
    }

    private Professor professorIn(String name, ResearchField... researchFields) {
        Professor professor = new Professor(name, name.toLowerCase() + "@carleton.ca");
        professor.getResearchFields().addAll(List.of(researchFields));
        return em.persist(professor);
    }

    @Test
    void submitRecordsASubmittedApplicationWithTimestamp() {
        Long id = service.submit(ada, info, ml.getId(), List.of(alice.getId()), NOW).getId();
        em.flush();
        em.clear();

        Application saved = applications.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(saved.getSubmittedAt()).isEqualTo(NOW);
        assertThat(saved.getCycle().getName()).isEqualTo("Fall 2027");
        assertThat(saved.getDesiredField()).isEqualTo(ml);
        assertThat(saved.getPersonalInfo().getCitizenship()).isEqualTo("Canada");
        assertThat(saved.getPreferredProfessors()).extracting(User::getName).containsExactly("Alice");
    }

    @Test
    void submitLinksTheApplicationToTheGivenApplicant() {
        Application saved = service.submit(ada, info, ml.getId(), List.of(alice.getId()), NOW);

        assertThat(saved.getApplicant().getId()).isEqualTo(ada.getId());
    }

    @Test
    void submitUsesTheAccountsEmailNotTheOneOnTheForm() {
        PersonalInfo otherEmail = new PersonalInfo("Ada", "Lovelace", "someone.else@example.com", null, null);

        Application saved = service.submit(ada, otherEmail, ml.getId(), List.of(alice.getId()), NOW);

        assertThat(saved.getPersonalInfo().getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void submitIgnoresRepeatedProfessors() {
        Application saved = service.submit(ada, info, ml.getId(), List.of(alice.getId(), alice.getId()), NOW);

        assertThat(saved.getPreferredProfessors()).hasSize(1);
    }

    @Test
    void submitRejectsAProfessorFromAnotherField() {
        ResearchField networks = em.persist(new ResearchField("Networks"));
        Professor bob = professorIn("Bob", networks);

        assertThatThrownBy(() -> service.submit(ada, info, ml.getId(), List.of(bob.getId()), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bob");
    }

    @Test
    void submitRejectsMoreThanThreeProfessors() {
        List<Long> ids = List.of(alice.getId(), professorIn("B", ml).getId(), professorIn("C", ml).getId(),
                professorIn("D", ml).getId());

        assertThatThrownBy(() -> service.submit(ada, info, ml.getId(), ids, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void submitRejectsNoProfessors() {
        assertThatThrownBy(() -> service.submit(ada, info, ml.getId(), List.of(), NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void submitRejectsAnUnknownField() {
        assertThatThrownBy(() -> service.submit(ada, info, 999_999L, List.of(alice.getId()), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitRejectsAnUnknownProfessor() {
        assertThatThrownBy(() -> service.submit(ada, info, ml.getId(), List.of(999_999L), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitRejectsAfterTheApplicationDeadline() {
        LocalDateTime dayAfter = cycle.getApplicationDeadline().plusDays(1).atStartOfDay();

        assertThatThrownBy(() -> service.submit(ada, info, ml.getId(), List.of(alice.getId()), dayAfter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The application deadline has passed");
    }

    @Test
    void submitRejectsWhenNoCycleIsActive() {
        cycle.setActive(false);
        em.flush();

        assertThatThrownBy(() -> service.submit(ada, info, ml.getId(), List.of(alice.getId()), NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Applications are not open right now");
    }

    @Test
    void professorsForFieldListsOnlyThatFieldsProfessorsByName() {
        ResearchField networks = em.persist(new ResearchField("Networks"));
        professorIn("Zed", ml);
        professorIn("Bob", networks);

        assertThat(service.professorsForField(ml.getId())).extracting(User::getName)
                .containsExactly("Alice", "Zed");
    }

    @Test
    void researchFieldsAreListedByName() {
        em.persist(new ResearchField("Algorithms"));

        assertThat(service.researchFields()).extracting(ResearchField::getName)
                .containsExactly("Algorithms", "Machine Learning");
    }

    @Test
    void findApplicationOfReturnsTheApplicantsOwnApplication() {
        Long id = service.submit(ada, info, ml.getId(), List.of(alice.getId()), NOW).getId();

        assertThat(service.findApplicationOf(id, ada)).isPresent();
        assertThat(service.findApplicationOf(999_999L, ada)).isEmpty();
    }

    @Test
    void findApplicationOfHidesAnotherApplicantsApplication() {
        Long id = service.submit(ada, info, ml.getId(), List.of(alice.getId()), NOW).getId();
        Applicant grace = em.persist(new Applicant("Grace Hopper", "grace@example.com"));

        assertThat(service.findApplicationOf(id, grace)).isEmpty();
    }
}