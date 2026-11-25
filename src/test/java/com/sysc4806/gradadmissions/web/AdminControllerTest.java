package com.sysc4806.gradadmissions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.sysc4806.gradadmissions.model.*;
import com.sysc4806.gradadmissions.config.SecurityConfig;
import com.sysc4806.gradadmissions.service.AdminApplicationService;
import com.sysc4806.gradadmissions.service.NotificationService;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class AdminControllerTest {

    private static final LocalDateTime NOV_7 = LocalDateTime.of(2026, 11, 7, 12, 0);

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminApplicationService service;
    @MockitoBean private NotificationService notifications;

    /** A submitted application with id 5 by Ada Lovelace, in Machine Learning, preferring Alice Chen. */
    private Application submittedApplication() {
        AdmissionCycle cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
        Application application = new Application(new Applicant("Ada Lovelace", "ada@example.com"), cycle,
                new PersonalInfo("Ada", "Lovelace", "ada@example.com", "613-555-0100", "Canada"));
        ReflectionTestUtils.setField(application, "id", 5L);
        application.setDesiredField(new ResearchField("Machine Learning"));
        Professor alice = new Professor("Alice Chen", "alice@carleton.ca");
        alice.setId(7L);
        application.addPreferredProfessor(alice);
        application.submit(NOV_7);
        return application;
    }

    @Test
    void listShowsEachApplicationInTheTable() throws Exception {
        when(service.submittedApplications(null, null, null, null, null)).thenReturn(List.of(submittedApplication()));

        mockMvc.perform(get("/admin/applications"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/applications"))
                .andExpect(content().string(containsString("Ada Lovelace")))
                .andExpect(content().string(containsString("Machine Learning")))
                .andExpect(content().string(containsString("Alice Chen")))
                .andExpect(content().string(containsString("2026-11-07 12:00")))
                .andExpect(content().string(containsString("SUBMITTED")))
                .andExpect(content().string(containsString("href=\"/admin/applications/5\"")))
                .andExpect(content().string(not(containsString("No submitted applications match these filters"))));
    }

    @Test
    void listShowsAMessageWhenThereAreNoApplications() throws Exception {
        when(service.submittedApplications(null, null, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/admin/applications"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No submitted applications match these filters")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    void filtersArePassedToTheServiceAndStaySelected() throws Exception {
        ResearchField field = new ResearchField("Machine Learning");
        ReflectionTestUtils.setField(field, "id", 3L);
        when(service.researchFields()).thenReturn(List.of(field));
        when(service.allProfessors()).thenReturn(List.of(professor(7L, "Alice Chen")));
        when(service.submittedApplications(3L, 7L, ApplicationStatus.SUBMITTED,
                NOV_7.toLocalDate(), NOV_7.toLocalDate())).thenReturn(List.of(submittedApplication()));

        String html = mockMvc.perform(get("/admin/applications").param("fieldId", "3")
                        .param("professorId", "7").param("status", "SUBMITTED")
                        .param("submittedFrom", "2026-11-07").param("submittedTo", "2026-11-07"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html).containsPattern("value=\"3\"[^>]*selected")
                .containsPattern("value=\"7\"[^>]*selected")
                .containsPattern("value=\"SUBMITTED\"[^>]*selected")
                .contains("value=\"2026-11-07\"", "Ada Lovelace");
        verify(service).submittedApplications(3L, 7L, ApplicationStatus.SUBMITTED,
                NOV_7.toLocalDate(), NOV_7.toLocalDate());
    }

    @Test
    void invalidDateRangesAndUnknownStatusesReturnBadRequest() throws Exception {
        when(service.submittedApplications(null, null, null, NOV_7.toLocalDate(), NOV_7.minusDays(1).toLocalDate()))
                .thenThrow(new IllegalArgumentException("The start date must be on or before the end date"));
        mockMvc.perform(get("/admin/applications").param("submittedFrom", "2026-11-07")
                        .param("submittedTo", "2026-11-06")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/admin/applications").param("status", "UNKNOWN")).andExpect(status().isBadRequest());
    }

    @Test
    void detailShowsTheWholeApplication() throws Exception {
        when(service.findSubmittedApplication(5L)).thenReturn(Optional.of(submittedApplication()));

        mockMvc.perform(get("/admin/applications/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/application"))
                .andExpect(content().string(containsString("Ada Lovelace")))
                .andExpect(content().string(containsString("ada@example.com")))
                .andExpect(content().string(containsString("613-555-0100")))
                .andExpect(content().string(containsString("Canada")))
                .andExpect(content().string(containsString("Fall 2027")))
                .andExpect(content().string(containsString("Machine Learning")))
                .andExpect(content().string(containsString("Alice Chen")))
                .andExpect(content().string(containsString("SUBMITTED")));
    }

    @Test
    void detailForAnUnknownApplicationIsNotFound() throws Exception {
        when(service.findSubmittedApplication(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/applications/99"))
                .andExpect(status().isNotFound());
    }

    // Routing and screening out (#39)

    private Professor professor(Long id, String name) {
        Professor professor = new Professor(name, name.toLowerCase().replace(' ', '.') + "@carleton.ca");
        professor.setId(id);
        return professor;
    }

    @Test
    void aSubmittedApplicationOffersRoutingWithThePreferredProfessorsSelectedAndScreeningOut() throws Exception {
        Application application = submittedApplication();
        when(service.findSubmittedApplication(5L)).thenReturn(Optional.of(application));
        when(service.allProfessors()).thenReturn(List.of(professor(7L, "Alice Chen"), professor(8L, "Brian Okonkwo")));

        String html = mockMvc.perform(get("/admin/applications/5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Not routed to any professor yet")))
                .andExpect(content().string(containsString("action=\"/admin/applications/5/route\"")))
                .andExpect(content().string(containsString("action=\"/admin/applications/5/screen-out\"")))
                .andExpect(content().string(containsString("Brian Okonkwo")))
                .andReturn().getResponse().getContentAsString();

        // Alice is preferred, so her checkbox is ticked; Brian's isn't.
        assertThat(html).containsPattern("value=\"7\"\\s+checked");
        assertThat(html).doesNotContainPattern("value=\"8\"\\s+checked");
    }

    @Test
    void anApplicationUnderReviewShowsEachProfessorsResponseAndOnlyOffersTheOthers() throws Exception {
        Application application = submittedApplication();
        application.startReview();
        Professor alice = application.getPreferredProfessors().iterator().next();
        Professor brian = professor(8L, "Brian Okonkwo");
        Evaluation aliceEvaluation = new Evaluation(application, alice, NOV_7.plusDays(1));
        aliceEvaluation.submitAssessment(Recommendation.RECOMMEND_WITH_FUNDING, "Strong candidate.", NOV_7.plusDays(2));
        Evaluation brianEvaluation = new Evaluation(application, brian, NOV_7.plusDays(1));
        when(service.findSubmittedApplication(5L)).thenReturn(Optional.of(application));
        when(service.evaluationsFor(5L)).thenReturn(List.of(aliceEvaluation, brianEvaluation));
        when(service.allProfessors()).thenReturn(List.of(alice, brian, professor(9L, "Carla Rossi")));

        String html = mockMvc.perform(get("/admin/applications/5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("UNDER_REVIEW")))
                .andExpect(content().string(containsString("Recommend, and yes to funding")))
                .andExpect(content().string(containsString("Strong candidate.")))
                .andExpect(content().string(containsString("Pending")))
                .andExpect(content().string(containsString("Route to more professors")))
                .andExpect(content().string(not(containsString("screen-out"))))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("value=\"9\"");
        assertThat(html).doesNotContain("value=\"7\"").doesNotContain("value=\"8\"");
    }

    @Test
    void aScreenedOutApplicationOffersNoActions() throws Exception {
        Application application = submittedApplication();
        application.screenOut();
        when(service.findSubmittedApplication(5L)).thenReturn(Optional.of(application));

        mockMvc.perform(get("/admin/applications/5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SCREENED_OUT")))
                .andExpect(content().string(not(containsString("/route\""))))
                .andExpect(content().string(not(containsString("screen-out"))));
    }

    @Test
    void routingEmailsTheProfessorsAndRedirectsWithTheirNames() throws Exception {
        Application application = submittedApplication();
        List<Evaluation> created = List.of(new Evaluation(application, professor(7L, "Alice Chen"), NOV_7),
                new Evaluation(application, professor(8L, "Brian Okonkwo"), NOV_7));
        when(service.route(eq(5L), eq(List.of(7L, 8L)), any())).thenReturn(created);
        when(notifications.notifyRouted(eq(created), any())).thenReturn(new NotificationService.Result(2, 0, 0, true));

        mockMvc.perform(post("/admin/applications/5/route").with(csrf()).param("professorIds", "7", "8"))
                .andExpect(redirectedUrl("/admin/applications/5"))
                .andExpect(flash().attribute("message", "Routed to Alice Chen, Brian Okonkwo. Emails: 2 sent, 0 failed, 0 skipped."));
        verify(notifications).notifyRouted(eq(created), any());
    }

    @Test
    void failedAndSkippedEmailsAreReportedButTheRoutingStands() throws Exception {
        Application application = submittedApplication();
        List<Evaluation> created = List.of(new Evaluation(application, professor(7L, "Alice Chen"), NOV_7));
        when(service.route(eq(5L), any(), any())).thenReturn(created);
        when(notifications.notifyRouted(any(), any())).thenReturn(new NotificationService.Result(0, 1, 2, true));

        mockMvc.perform(post("/admin/applications/5/route").with(csrf()).param("professorIds", "7"))
                .andExpect(redirectedUrl("/admin/applications/5"))
                .andExpect(flash().attribute("message", "Routed to Alice Chen. Emails: 0 sent, 1 failed, 2 skipped."))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    void aFailedRoutingSendsNoEmail() throws Exception {
        when(service.route(eq(5L), any(), any())).thenThrow(new IllegalStateException("Already routed"));

        mockMvc.perform(post("/admin/applications/5/route").with(csrf()).param("professorIds", "7"))
                .andExpect(flash().attribute("error", "Already routed"));
        verify(notifications, never()).notifyRouted(any(), any());
    }

    @Test
    void theListShowsTheEmailCircuitBreakerState() throws Exception {
        when(notifications.status()).thenReturn(new NotificationService.Status(true, CircuitBreaker.State.OPEN));

        mockMvc.perform(get("/admin/applications"))
                .andExpect(content().string(containsString("Email circuit breaker:")))
                .andExpect(content().string(containsString("OPEN")));
    }

    @Test
    void theDetailPageShowsWhetherEachProfessorWasEmailed() throws Exception {
        Application application = submittedApplication();
        application.startReview();
        Evaluation emailed = new Evaluation(application, professor(7L, "Alice Chen"), NOV_7);
        emailed.markNotified(NOV_7.plusMinutes(1));
        Evaluation notEmailed = new Evaluation(application, professor(8L, "Brian Okonkwo"), NOV_7);
        when(service.findSubmittedApplication(5L)).thenReturn(Optional.of(application));
        when(service.evaluationsFor(5L)).thenReturn(List.of(emailed, notEmailed));

        mockMvc.perform(get("/admin/applications/5"))
                .andExpect(content().string(containsString("2026-11-07 12:01")))
                .andExpect(content().string(containsString("Not sent")));
    }

    @Test
    void aRoutingErrorIsShownOnTheDetailPage() throws Exception {
        when(service.route(eq(5L), any(), any()))
                .thenThrow(new IllegalArgumentException("Choose at least one professor"));

        mockMvc.perform(post("/admin/applications/5/route").with(csrf()))
                .andExpect(redirectedUrl("/admin/applications/5"))
                .andExpect(flash().attribute("error", "Choose at least one professor"));
    }

    @Test
    void theFlashMessageIsShownOnTheDetailPage() throws Exception {
        when(service.findSubmittedApplication(5L)).thenReturn(Optional.of(submittedApplication()));

        mockMvc.perform(get("/admin/applications/5").flashAttr("error", "Choose at least one professor"))
                .andExpect(content().string(containsString("Choose at least one professor")));
    }

    @Test
    void screeningOutRedirectsWithAConfirmation() throws Exception {
        mockMvc.perform(post("/admin/applications/5/screen-out").with(csrf()))
                .andExpect(redirectedUrl("/admin/applications/5"))
                .andExpect(flash().attribute("message", "The application was screened out."));
        verify(service).screenOut(5L);
    }

    @Test
    void aScreenOutErrorIsShownOnTheDetailPage() throws Exception {
        doThrow(new IllegalStateException("Only a submitted application that hasn't been routed can be screened out"))
                .when(service).screenOut(5L);

        mockMvc.perform(post("/admin/applications/5/screen-out").with(csrf()))
                .andExpect(redirectedUrl("/admin/applications/5"))
                .andExpect(flash().attribute("error",
                        "Only a submitted application that hasn't been routed can be screened out"));
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    void onlyAdminsCanRouteOrScreenOut() throws Exception {
        mockMvc.perform(post("/admin/applications/5/route").with(csrf()).param("professorIds", "7"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/applications/5/screen-out").with(csrf()))
                .andExpect(status().isForbidden());
        verify(service, never()).route(any(), any(), any());
        verify(service, never()).screenOut(any());
    }
}
