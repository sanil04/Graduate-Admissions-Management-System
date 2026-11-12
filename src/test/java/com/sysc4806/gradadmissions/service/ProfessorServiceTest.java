package com.sysc4806.gradadmissions.service;

import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfessorServiceTest {

    @Mock
    private ProfessorRepository professorRepository;

    @InjectMocks
    private ProfessorService professorService;

    @Test
    public void testFindById() {
        Professor prof = new Professor();
        prof.setId(1L);
        prof.setName("Dr. Smith");

        when(professorRepository.findById(1L)).thenReturn(Optional.of(prof));

        Professor found = professorService.findById(1L);
        assertEquals("Dr. Smith", found.getName());
        verify(professorRepository, times(1)).findById(1L);
    }
}