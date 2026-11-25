package com.sysc4806.gradadmissions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.sysc4806.gradadmissions.model.*;
import com.sysc4806.gradadmissions.config.SecurityConfig;
import com.sysc4806.gradadmissions.service.ApplicationService;
import com.sysc4806.gradadmissions.service.CurrentUserService;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "APPLICANT")
class ApplicationControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 12, 0);

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ApplicationService service;
    @MockitoBean private CurrentUserService currentUsers;

    private Applicant ada;

    @BeforeEach
    void logInAsAda() {
        ada = new Applicant("Ada Lovelace", "ada@example.com");
        ada.setId(1L);
        when(currentUsers.currentApplicant()).thenReturn(ada);
    }

    private ResearchField field(Long id, String name) {
        ResearchField field = new ResearchField(name);
        ReflectionTestUtils.setField(field, "id", id);
        return field;
    }

    private Professor professor(Long id, String name) {
        Professor professor = new Professor(name, name.toLowerCase().replace(' ', '.') + "@carleton.ca");
        professor.setId(id);
        return professor;
    }

    private MockHttpServletRequestBuilder validSubmission() {
        return post("/apply").with(csrf())
                .param("personalInfo.firstName", "Ada")
                .param("personalInfo.lastName", "Lovelace")
                .param("personalInfo.email", "ada@example.com")
                .param("desiredFieldId", "1")
                .param("professorIds", "2");
    }

    @Test
    void formListsTheResearchFieldsFromTheService() throws Exception {
        when(service.researchFields()).thenReturn(List.of(field(1L, "Machine Learning")));

        mockMvc.perform(get("/apply"))
                .andExpect(status().isOk())
                .andExpect(view().name("applicant/apply"))
                .andExpect(content().string(containsString("Machine Learning")))
                .andExpect(content().string(containsString("name=\"personalInfo.firstName\"")))
                .andExpect(content().string(containsString("name=\"professorIds\"")));
    }

    @Test
    void formIsFilledInFromTheLoggedInApplicantsAccount() throws Exception {
        mockMvc.perform(get("/apply"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("form", hasProperty("personalInfo", allOf(
                        hasProperty("firstName", is("Ada")),
                        hasProperty("lastName", is("Lovelace")),
                        hasProperty("email", is("ada@example.com"))))))
                .andExpect(content().string(matchesPattern(
                        "(?s).*<input[^>]*id=\"email\"[^>]*readonly[^>]*value=\"ada@example.com\".*")));
    }

    @Test
    void aForgedEmailInTheRequestIsIgnored() throws Exception {
        Application saved = new Application(ada, null, new PersonalInfo());
        ReflectionTestUtils.setField(saved, "id", 5L);
        ArgumentCaptor<PersonalInfo> info = ArgumentCaptor.forClass(PersonalInfo.class);
        when(service.submit(eq(ada), info.capture(), eq(1L), eq(List.of(2L)), any(LocalDateTime.class)))
                .thenReturn(saved);

        mockMvc.perform(post("/apply").with(csrf())
                        .param("personalInfo.firstName", "Ada")
                        .param("personalInfo.lastName", "Lovelace")
                        .param("personalInfo.email", "mallory@example.com")
                        .param("desiredFieldId", "1")
                        .param("professorIds", "2"))
                .andExpect(redirectedUrl("/apply/confirmation/5"));

        assertThat(info.getValue().getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void theAccountNameIsSplitIntoFirstAndLastName() {
        PersonalInfo twoParts = ApplicationController.personalInfoFor(new Applicant("Ada King Lovelace", "ada@example.com"));
        PersonalInfo onePart = ApplicationController.personalInfoFor(new Applicant("Plato", "plato@example.com"));

        assertThat(twoParts.getFirstName()).isEqualTo("Ada");
        assertThat(twoParts.getLastName()).isEqualTo("King Lovelace");
        assertThat(onePart.getFirstName()).isEqualTo("Plato");
        assertThat(onePart.getLastName()).isEmpty();
    }

    @Test
    void professorsEndpointReturnsTheProfessorsOfTheChosenField() throws Exception {
        when(service.professorsForField(1L)).thenReturn(List.of(professor(7L, "Alice Chen")));

        mockMvc.perform(get("/apply/professors").param("fieldId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Alice Chen"));
    }

    @Test
    void validSubmissionRedirectsToTheConfirmationPage() throws Exception {
        Application saved = new Application(new Applicant("Ada Lovelace", "ada@example.com"), null,
                new PersonalInfo("Ada", "Lovelace", "ada@example.com", null, null));
        ReflectionTestUtils.setField(saved, "id", 5L);
        when(service.submit(eq(ada), any(PersonalInfo.class), eq(1L), eq(List.of(2L)), any(LocalDateTime.class)))
                .thenReturn(saved);

        mockMvc.perform(validSubmission())
                .andExpect(redirectedUrl("/apply/confirmation/5"));
    }

    @Test
    void invalidSubmissionRedisplaysTheFormWithoutCallingTheService() throws Exception {
        mockMvc.perform(post("/apply").with(csrf())
                        .param("personalInfo.firstName", "")
                        .param("personalInfo.lastName", "")
                        // Ignored, so never a field error: the email comes from the account.
                        .param("personalInfo.email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("applicant/apply"))
                .andExpect(model().attributeHasFieldErrors("form", "personalInfo.firstName",
                        "personalInfo.lastName", "desiredFieldId", "professorIds"));

        verify(service, never()).submit(any(), any(), any(), any(), any());
    }

    @Test
    void moreThanThreeProfessorsIsRejected() throws Exception {
        mockMvc.perform(validSubmission().param("professorIds", "3", "4", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("applicant/apply"))
                .andExpect(model().attributeHasFieldErrors("form", "professorIds"));

        verify(service, never()).submit(any(), any(), any(), any(), any());
    }

    @Test
    void ruleViolationsFromTheServiceAreShownOnTheForm() throws Exception {
        when(service.submit(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("The application deadline has passed"));

        mockMvc.perform(validSubmission())
                .andExpect(status().isOk())
                .andExpect(view().name("applicant/apply"))
                .andExpect(content().string(containsString("The application deadline has passed")));
    }

    @Test
    void confirmationShowsTheSubmittedApplication() throws Exception {
        AdmissionCycle cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
        Application application = new Application(new Applicant("Ada Lovelace", "ada@example.com"), cycle,
                new PersonalInfo("Ada", "Lovelace", "ada@example.com", null, null));
        application.setDesiredField(field(1L, "Machine Learning"));
        application.addPreferredProfessor(professor(7L, "Alice Chen"));
        application.submit(NOW);
        when(service.findApplicationOf(5L, ada)).thenReturn(Optional.of(application));

        mockMvc.perform(get("/apply/confirmation/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("applicant/confirmation"))
                .andExpect(content().string(containsString("SUBMITTED")))
                .andExpect(content().string(containsString("2026-11-07 12:00")))
                .andExpect(content().string(containsString("Machine Learning")))
                .andExpect(content().string(containsString("Alice Chen")));
    }

    @Test
    void confirmationIsNotFoundUnlessItIsTheApplicantsOwnApplication() throws Exception {
        // Empty both for an unknown id and for another applicant's application (see ApplicationServiceTest).
        when(service.findApplicationOf(99L, ada)).thenReturn(Optional.empty());

        mockMvc.perform(get("/apply/confirmation/99"))
                .andExpect(status().isNotFound());
    }
}