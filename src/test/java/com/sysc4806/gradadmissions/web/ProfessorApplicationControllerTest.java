package com.sysc4806.gradadmissions.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
}