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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.sysc4806.gradadmissions.model.*;
import com.sysc4806.gradadmissions.service.AdminApplicationService;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    private static final LocalDateTime NOV_7 = LocalDateTime.of(2026, 11, 7, 12, 0);

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminApplicationService service;

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
        when(service.submittedApplications()).thenReturn(List.of(submittedApplication()));

        mockMvc.perform(get("/admin/applications"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/applications"))
                .andExpect(content().string(containsString("Ada Lovelace")))
                .andExpect(content().string(containsString("Machine Learning")))
                .andExpect(content().string(containsString("Alice Chen")))
                .andExpect(content().string(containsString("2026-11-07 12:00")))
                .andExpect(content().string(containsString("SUBMITTED")))
                .andExpect(content().string(containsString("href=\"/admin/applications/5\"")))
                .andExpect(content().string(not(containsString("No applications have been submitted yet"))));
    }

    @Test
    void listShowsAMessageWhenThereAreNoApplications() throws Exception {
        when(service.submittedApplications()).thenReturn(List.of());

        mockMvc.perform(get("/admin/applications"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No applications have been submitted yet")))
                .andExpect(content().string(not(containsString("<table"))));
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
}
