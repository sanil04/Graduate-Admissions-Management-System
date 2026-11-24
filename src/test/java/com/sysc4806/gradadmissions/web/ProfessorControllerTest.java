package com.sysc4806.gradadmissions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

import com.sysc4806.gradadmissions.config.SecurityConfig;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;
import com.sysc4806.gradadmissions.service.CurrentUserService;
import com.sysc4806.gradadmissions.service.ProfessorService;
import com.sysc4806.gradadmissions.web.dto.ProfessorFormDto;

@WebMvcTest(ProfessorController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "PROFESSOR")
class ProfessorControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProfessorService service;
    @MockitoBean private CurrentUserService currentUsers;

    private ResearchField field(Long id, String name) {
        ResearchField field = new ResearchField(name);
        ReflectionTestUtils.setField(field, "id", id);
        return field;
    }

    /**
     * Professor 1, Dr. Alan Turing, works in AI (3) and is the one logged in; Networks (4) also
     * exists. Professor 2, Grace Hopper, is another professor.
     */
    @BeforeEach
    void setUp() {
        ResearchField ai = field(3L, "AI");
        Professor turing = new Professor("Dr. Alan Turing", "turing@carleton.ca");
        turing.setId(1L);
        turing.getResearchFields().add(ai);
        when(service.findById(1L)).thenReturn(Optional.of(turing));
        Professor hopper = new Professor("Grace Hopper", "hopper@carleton.ca");
        hopper.setId(2L);
        when(service.findById(2L)).thenReturn(Optional.of(hopper));
        when(service.findAll()).thenReturn(List.of(turing, hopper));
        when(currentUsers.currentProfessor()).thenReturn(turing);
        when(service.researchFields()).thenReturn(List.of(ai, field(4L, "Networks")));
    }

    @Test
    void listLinksToEachProfile() throws Exception {
        mockMvc.perform(get("/professor/profiles"))
                .andExpect(status().isOk())
                .andExpect(view().name("professor/list"))
                .andExpect(content().string(containsString("href=\"/professor/1\"")))
                .andExpect(content().string(containsString("Dr. Alan Turing")));
    }

    @Test
    void profileShowsNameEmailAndResearchFields() throws Exception {
        mockMvc.perform(get("/professor/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("professor/profile"))
                .andExpect(content().string(containsString("Dr. Alan Turing")))
                .andExpect(content().string(containsString("turing@carleton.ca")))
                .andExpect(content().string(containsString("<li>AI</li>")))
                .andExpect(content().string(containsString("href=\"/professor/1/edit\"")));
    }

    @Test
    void unknownProfessorIsNotFound() throws Exception {
        mockMvc.perform(get("/professor/99")).andExpect(status().isNotFound());
        mockMvc.perform(get("/professor/99/edit")).andExpect(status().isNotFound());
        mockMvc.perform(post("/professor/99/edit").with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void editFormIsPrefilledWithTheCurrentFieldsSelected() throws Exception {
        mockMvc.perform(get("/professor/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("professor/form"))
                .andExpect(content().string(containsString("value=\"Dr. Alan Turing\"")))
                .andExpect(content().string(containsString("<option value=\"3\" selected=\"selected\">AI</option>")))
                .andExpect(content().string(containsString("<option value=\"4\">Networks</option>")));
    }

    @Test
    void validUpdateIsSavedAndRedirectsToTheProfile() throws Exception {
        mockMvc.perform(post("/professor/1/edit").with(csrf())
                        .param("name", "Alan Turing")
                        .param("email", "alan@carleton.ca")
                        .param("researchFieldIds", "3", "4"))
                .andExpect(redirectedUrl("/professor/1"));

        ArgumentCaptor<ProfessorFormDto> form = ArgumentCaptor.forClass(ProfessorFormDto.class);
        verify(service).update(eq(1L), form.capture());
        assertThat(form.getValue().getName()).isEqualTo("Alan Turing");
        assertThat(form.getValue().getEmail()).isEqualTo("alan@carleton.ca");
        assertThat(form.getValue().getResearchFieldIds()).isEqualTo(Set.of(3L, 4L));
    }

    @Test
    void validationErrorsAreShownOnTheFormWithoutSaving() throws Exception {
        mockMvc.perform(post("/professor/1/edit").with(csrf()).param("name", "").param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("professor/form"))
                .andExpect(model().attributeHasFieldErrors("form", "name", "email"))
                .andExpect(content().string(containsString("Name cannot be blank")))
                .andExpect(content().string(containsString("Invalid email format")));

        verify(service, never()).update(any(), any());
    }

    @Test
    void submittingWithNoResearchFieldsShowsAnErrorAndDoesNotSave() throws Exception {
        mockMvc.perform(post("/professor/1/edit").with(csrf())
                        .param("name", "Alan Turing")
                        .param("email", "alan@carleton.ca"))
                .andExpect(status().isOk())
                .andExpect(view().name("professor/form"))
                .andExpect(model().attributeHasFieldErrors("form", "researchFieldIds"))
                .andExpect(content().string(containsString("Select at least one research field")));

        verify(service, never()).update(any(), any());
    }

    @Test
    void aServiceSideNoFieldsRejectionIsShownOnTheForm() throws Exception {
        when(service.update(eq(1L), any())).thenThrow(new ProfessorService.NoResearchFieldsException());

        mockMvc.perform(post("/professor/1/edit").with(csrf())
                        .param("name", "Alan Turing")
                        .param("email", "alan@carleton.ca")
                        .param("researchFieldIds", "99"))
                .andExpect(status().isOk())
                .andExpect(view().name("professor/form"))
                .andExpect(model().attributeHasFieldErrors("form", "researchFieldIds"))
                .andExpect(content().string(containsString("Select at least one research field")));
    }

    @Test
    void anEmailUsedByAnotherAccountIsShownOnTheForm() throws Exception {
        when(service.update(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("That email address belongs to another account"));

        mockMvc.perform(post("/professor/1/edit").with(csrf()).param("name", "Alan Turing").param("email", "taken@carleton.ca")
                        .param("researchFieldIds", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("professor/form"))
                .andExpect(content().string(containsString("That email address belongs to another account")));
    }

    @Test
    void anotherProfessorsProfileHasNoEditLink() throws Exception {
        mockMvc.perform(get("/professor/2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Grace Hopper")))
                .andExpect(content().string(not(containsString("href=\"/professor/2/edit\""))));
    }

    @Test
    void aProfessorCannotEditAnotherProfessorsProfile() throws Exception {
        mockMvc.perform(get("/professor/2/edit")).andExpect(status().isForbidden());
        mockMvc.perform(post("/professor/2/edit").with(csrf())
                        .param("name", "Hacked")
                        .param("email", "hacked@carleton.ca"))
                .andExpect(status().isForbidden());

        verify(service, never()).update(any(), any());
    }
}
