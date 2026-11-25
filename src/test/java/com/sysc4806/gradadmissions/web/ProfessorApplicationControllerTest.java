package com.sysc4806.gradadmissions.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.sysc4806.gradadmissions.config.SecurityConfig;
import com.sysc4806.gradadmissions.model.*;
import com.sysc4806.gradadmissions.service.CurrentUserService;
import com.sysc4806.gradadmissions.service.ProfessorApplicationService;

@WebMvcTest(ProfessorApplicationController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "PROFESSOR")
class ProfessorApplicationControllerTest {

    private static final LocalDateTime NOV_7 = LocalDateTime.of(2026, 11, 7, 12, 0);
    private static final LocalDateTime NOV_20 = LocalDateTime.of(2026, 11, 20, 9, 0);

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProfessorApplicationService service;
    @MockitoBean private CurrentUserService currentUsers;

    private Professor alice;

    @BeforeEach
    void setUp() {
        alice = new Professor("Alice Chen", "alice@carleton.ca");
        alice.setId(7L);
        when(currentUsers.currentProfessor()).thenReturn(alice);
    }

    /** Ada Lovelace's submitted application (id 5, Machine Learning) routed to Alice. */
    private Evaluation evaluation() {
        AdmissionCycle cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
        Application application = new Application(new Applicant("Ada Lovelace", "ada@example.com"), cycle,
                new PersonalInfo("Ada", "Lovelace", "ada@example.com", "613-555-0100", "Canada"));
        ReflectionTestUtils.setField(application, "id", 5L);
        application.setDesiredField(new ResearchField("Machine Learning"));
        application.addPreferredProfessor(alice);
        application.submit(NOV_7);
        return new Evaluation(application, alice, NOV_20);
    }

    @Test
    void listShowsEachAssignedApplicationWithPendingStatus() throws Exception {
        when(service.assignedTo(7L)).thenReturn(List.of(evaluation()));

        mockMvc.perform(get("/professor/applications"))
                .andExpect(status().isOk())
                .andExpect(view().name("professor/applications"))
                .andExpect(content().string(containsString("Ada Lovelace")))
                .andExpect(content().string(containsString("Machine Learning")))
                .andExpect(content().string(containsString("2026-11-20 09:00")))
                .andExpect(content().string(containsString("Pending")))
                .andExpect(content().string(containsString("href=\"/professor/applications/5\"")))
                .andExpect(content().string(not(containsString("No applications have been routed"))));
    }

    @Test
    void listShowsSubmittedStatusOnceAssessed() throws Exception {
        Evaluation evaluation = evaluation();
        evaluation.submitAssessment(Recommendation.RECOMMEND_NO_FUNDING, null, NOV_20.plusDays(1));
        when(service.assignedTo(7L)).thenReturn(List.of(evaluation));

        mockMvc.perform(get("/professor/applications"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Submitted")))
                .andExpect(content().string(not(containsString("Pending"))));
    }

    @Test
    void listSaysSoWhenNothingWasRouted() throws Exception {
        when(service.assignedTo(7L)).thenReturn(List.of());

        mockMvc.perform(get("/professor/applications"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No applications have been routed to you yet")));
    }

    @Test
    void detailShowsTheApplication() throws Exception {
        when(service.findAssigned(5L, 7L)).thenReturn(Optional.of(evaluation()));

        mockMvc.perform(get("/professor/applications/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("professor/application"))
                .andExpect(content().string(containsString("Ada Lovelace")))
                .andExpect(content().string(containsString("ada@example.com")))
                .andExpect(content().string(containsString("613-555-0100")))
                .andExpect(content().string(containsString("Canada")))
                .andExpect(content().string(containsString("Fall 2027")))
                .andExpect(content().string(containsString("Machine Learning")))
                .andExpect(content().string(containsString("Alice Chen")))
                .andExpect(content().string(containsString("2026-11-07 12:00")))
                .andExpect(content().string(containsString("Pending")));
    }

    @Test
    void detailShowsTheSubmittedAssessment() throws Exception {
        Evaluation evaluation = evaluation();
        evaluation.submitAssessment(Recommendation.RECOMMEND_WITH_FUNDING, "Strong candidate.", NOV_20.plusDays(1));
        when(service.findAssigned(5L, 7L)).thenReturn(Optional.of(evaluation));

        mockMvc.perform(get("/professor/applications/5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Recommend, and yes to funding")))
                .andExpect(content().string(containsString("Strong candidate.")));
    }

    @Test
    void anApplicationNotRoutedToTheProfessorIsNotFound() throws Exception {
        when(service.findAssigned(5L, 7L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/professor/applications/5")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "APPLICANT")
    void applicantsCannotOpenTheProfessorPages() throws Exception {
        mockMvc.perform(get("/professor/applications")).andExpect(status().isForbidden());
        mockMvc.perform(get("/professor/applications/5")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminsCannotOpenTheProfessorPages() throws Exception {
        mockMvc.perform(get("/professor/applications")).andExpect(status().isForbidden());
    }

    // Submitting an assessment (#42)

    @Test
    void aPendingApplicationShowsTheAssessmentFormWithTheFourRecommendations() throws Exception {
        when(service.findAssigned(5L, 7L)).thenReturn(Optional.of(evaluation()));

        String html = mockMvc.perform(get("/professor/applications/5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/professor/applications/5/assessment\"")))
                .andExpect(content().string(containsString("Don&#39;t recommend for admission")))
                .andExpect(content().string(containsString("Recommend, but not interested in supervision")))
                .andExpect(content().string(containsString("Recommend, but no funding")))
                .andExpect(content().string(containsString("Recommend, and yes to funding")))
                .andExpect(content().string(containsString("name=\"comments\"")))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(html.split("type=\"radio\"", -1)).hasSize(5);
        org.assertj.core.api.Assertions.assertThat(html)
                .contains("value=\"DO_NOT_RECOMMEND\"", "value=\"RECOMMEND_NO_SUPERVISION\"",
                        "value=\"RECOMMEND_NO_FUNDING\"", "value=\"RECOMMEND_WITH_FUNDING\"");
    }

    @Test
    void aSubmittedAssessmentIsReadOnlyWithNoForm() throws Exception {
        Evaluation evaluation = evaluation();
        evaluation.submitAssessment(Recommendation.RECOMMEND_WITH_FUNDING, "Strong candidate.", NOV_20.plusDays(1));
        when(service.findAssigned(5L, 7L)).thenReturn(Optional.of(evaluation));

        mockMvc.perform(get("/professor/applications/5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Recommend, and yes to funding")))
                .andExpect(content().string(containsString("Strong candidate.")))
                .andExpect(content().string(containsString("2026-11-21 09:00")))
                .andExpect(content().string(not(containsString("/assessment"))))
                .andExpect(content().string(not(containsString("type=\"radio\""))))
                .andExpect(content().string(not(containsString("<textarea"))));
    }

    @Test
    void submittingSavesTheAssessmentAndRedirectsBackToThePage() throws Exception {
        mockMvc.perform(post("/professor/applications/5/assessment").with(csrf())
                        .param("recommendation", "RECOMMEND_WITH_FUNDING")
                        .param("comments", "Strong candidate."))
                .andExpect(redirectedUrl("/professor/applications/5"))
                .andExpect(flash().attribute("message", "Your assessment was submitted."))
                .andExpect(flash().attributeCount(1));
        verify(service).submitAssessment(eq(5L), eq(7L), eq(Recommendation.RECOMMEND_WITH_FUNDING),
                eq("Strong candidate."), any());
    }

    @Test
    void commentsAreOptional() throws Exception {
        mockMvc.perform(post("/professor/applications/5/assessment").with(csrf())
                        .param("recommendation", "DO_NOT_RECOMMEND"))
                .andExpect(redirectedUrl("/professor/applications/5"))
                .andExpect(flash().attribute("message", "Your assessment was submitted."));
        verify(service).submitAssessment(eq(5L), eq(7L), eq(Recommendation.DO_NOT_RECOMMEND), isNull(), any());
    }

    @Test
    void submittingWithoutARecommendationShowsAnErrorAndKeepsTheComments() throws Exception {
        when(service.submitAssessment(eq(5L), eq(7L), isNull(), any(), any()))
                .thenThrow(new IllegalArgumentException("Choose a recommendation"));

        mockMvc.perform(post("/professor/applications/5/assessment").with(csrf()).param("comments", "Half written"))
                .andExpect(redirectedUrl("/professor/applications/5"))
                .andExpect(flash().attribute("error", "Choose a recommendation"))
                .andExpect(flash().attribute("draftComments", "Half written"))
                .andExpect(flash().attributeCount(2));
    }

    @Test
    void submittingTwiceShowsTheErrorInsteadOfOverwriting() throws Exception {
        when(service.submitAssessment(eq(5L), eq(7L), any(), any(), any()))
                .thenThrow(new IllegalStateException("This assessment has already been submitted"));

        mockMvc.perform(post("/professor/applications/5/assessment").with(csrf())
                        .param("recommendation", "DO_NOT_RECOMMEND"))
                .andExpect(redirectedUrl("/professor/applications/5"))
                .andExpect(flash().attribute("error", "This assessment has already been submitted"));
    }

    @Test
    void submittingForAnApplicationNotRoutedToTheProfessorIsNotFound() throws Exception {
        when(service.submitAssessment(eq(5L), eq(7L), any(), any(), any()))
                .thenThrow(new NoSuchElementException("This application was not routed to you"));

        mockMvc.perform(post("/professor/applications/5/assessment").with(csrf())
                        .param("recommendation", "DO_NOT_RECOMMEND"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anUnknownRecommendationIsABadRequest() throws Exception {
        mockMvc.perform(post("/professor/applications/5/assessment").with(csrf())
                        .param("recommendation", "SURE_WHY_NOT"))
                .andExpect(status().isBadRequest());
        verify(service, never()).submitAssessment(any(), any(), any(), any(), any());
    }

    @Test
    void theErrorAndTheTypedAssessmentAreShownAgainOnThePage() throws Exception {
        when(service.findAssigned(5L, 7L)).thenReturn(Optional.of(evaluation()));

        String html = mockMvc.perform(get("/professor/applications/5")
                        .flashAttr("error", "Comments can be at most 2000 characters")
                        .flashAttr("draftRecommendation", Recommendation.RECOMMEND_NO_FUNDING)
                        .flashAttr("draftComments", "Half written"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Comments can be at most 2000 characters")))
                .andExpect(content().string(containsString("Half written")))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(html)
                .containsPattern("value=\"RECOMMEND_NO_FUNDING\"\\s+checked")
                .doesNotContainPattern("value=\"DO_NOT_RECOMMEND\"\\s+checked");
    }

    @Test
    void theSuccessMessageIsShownOnThePage() throws Exception {
        Evaluation evaluation = evaluation();
        evaluation.submitAssessment(Recommendation.RECOMMEND_NO_FUNDING, null, NOV_20.plusDays(1));
        when(service.findAssigned(5L, 7L)).thenReturn(Optional.of(evaluation));

        mockMvc.perform(get("/professor/applications/5").flashAttr("message", "Your assessment was submitted."))
                .andExpect(content().string(containsString("Your assessment was submitted.")));
    }

    @Test
    void submittingWithoutACsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/professor/applications/5/assessment").param("recommendation", "DO_NOT_RECOMMEND"))
                .andExpect(status().isForbidden());
        verify(service, never()).submitAssessment(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "APPLICANT")
    void applicantsCannotSubmitAnAssessment() throws Exception {
        mockMvc.perform(post("/professor/applications/5/assessment").with(csrf())
                        .param("recommendation", "DO_NOT_RECOMMEND"))
                .andExpect(status().isForbidden());
        verify(service, never()).submitAssessment(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminsCannotSubmitAnAssessment() throws Exception {
        mockMvc.perform(post("/professor/applications/5/assessment").with(csrf())
                        .param("recommendation", "DO_NOT_RECOMMEND"))
                .andExpect(status().isForbidden());
        verify(service, never()).submitAssessment(any(), any(), any(), any(), any());
    }
}