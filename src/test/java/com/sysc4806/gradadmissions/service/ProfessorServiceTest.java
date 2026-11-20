package com.sysc4806.gradadmissions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sysc4806.gradadmissions.model.Admin;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;
import com.sysc4806.gradadmissions.web.dto.ProfessorFormDto;
import com.sysc4806.gradadmissions.web.dto.AdminProfessorFormDto;

@ExtendWith(MockitoExtension.class)
class ProfessorServiceTest {

    @Mock private ProfessorRepository professorRepository;
    @Mock private ResearchFieldRepository researchFieldRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private ProfessorService professorService;

    private Professor smith;
    private ProfessorFormDto form;

    /** Professor 1, Dr. Smith, works in AI; the form renames them and switches them to Networks. */
    @BeforeEach
    void setUp() {
        smith = new Professor("Dr. Smith", "smith@carleton.ca");
        smith.setId(1L);
        smith.getResearchFields().add(new ResearchField("AI"));
        form = new ProfessorFormDto();
        form.setName("Dr. Jane Smith");
        form.setEmail("jane.smith@carleton.ca");
        form.setResearchFieldIds(Set.of(4L));
    }

    @Test
    void findByIdReturnsTheProfessor() {
        when(professorRepository.findById(1L)).thenReturn(Optional.of(smith));

        assertThat(professorService.findById(1L)).contains(smith);
    }

    @Test
    void updateReplacesNameEmailAndResearchFields() {
        ResearchField networks = new ResearchField("Networks");
        when(professorRepository.findById(1L)).thenReturn(Optional.of(smith));
        when(userRepository.findByEmail("jane.smith@carleton.ca")).thenReturn(Optional.empty());
        when(researchFieldRepository.findAllById(Set.of(4L))).thenReturn(List.of(networks));
        when(professorRepository.save(smith)).thenReturn(smith);

        Professor updated = professorService.update(1L, form);

        assertThat(updated.getName()).isEqualTo("Dr. Jane Smith");
        assertThat(updated.getEmail()).isEqualTo("jane.smith@carleton.ca");
        assertThat(updated.getResearchFields()).containsExactly(networks);
    }

    @Test
    void updateKeepsTheProfessorsOwnEmail() {
        form.setEmail("smith@carleton.ca");
        when(professorRepository.findById(1L)).thenReturn(Optional.of(smith));
        when(userRepository.findByEmail("smith@carleton.ca")).thenReturn(Optional.of(smith));
        when(professorRepository.save(smith)).thenReturn(smith);

        assertThat(professorService.update(1L, form).getEmail()).isEqualTo("smith@carleton.ca");
    }

    @Test
    void updateRejectsAnEmailUsedByAnotherAccount() {
        Admin admin = new Admin("System Admin", "jane.smith@carleton.ca");
        admin.setId(2L);
        when(professorRepository.findById(1L)).thenReturn(Optional.of(smith));
        when(userRepository.findByEmail("jane.smith@carleton.ca")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> professorService.update(1L, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("That email address belongs to another account");
        verify(professorRepository, never()).save(smith);
    }

    @Test
    void createSavesAProfessorWithAnEncodedPassword() {
        AdminProfessorFormDto createForm = new AdminProfessorFormDto();
        createForm.setName("Dr. New");
        createForm.setEmail("new@carleton.ca");
        createForm.setTemporaryPassword("temp123");
        createForm.setResearchFieldIds(Set.of(4L));
        ResearchField networks = new ResearchField("Networks");

        when(userRepository.findByEmail("new@carleton.ca")).thenReturn(Optional.empty());
        when(researchFieldRepository.findAllById(Set.of(4L))).thenReturn(List.of(networks));
        when(passwordEncoder.encode("temp123")).thenReturn("encoded-hash");
        when(professorRepository.save(any(Professor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Professor created = professorService.create(createForm);

        assertThat(created.getName()).isEqualTo("Dr. New");
        assertThat(created.getEmail()).isEqualTo("new@carleton.ca");
        assertThat(created.getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(created.getResearchFields()).containsExactly(networks);
        verify(professorRepository).save(created);
    }

    @Test
    void createRejectsADuplicateEmail() {
        AdminProfessorFormDto createForm = new AdminProfessorFormDto();
        createForm.setName("Dr. New");
        createForm.setEmail("smith@carleton.ca");
        createForm.setTemporaryPassword("temp123");
        createForm.setResearchFieldIds(Set.of(4L));
        when(userRepository.findByEmail("smith@carleton.ca")).thenReturn(Optional.of(smith));

        assertThatThrownBy(() -> professorService.create(createForm))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("That email address belongs to another account");
        verify(professorRepository, never()).save(any());
    }
}
