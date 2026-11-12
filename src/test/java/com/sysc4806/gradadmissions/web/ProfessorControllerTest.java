package com.sysc4806.gradadmissions.web;

import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;
import com.sysc4806.gradadmissions.service.ProfessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfessorController.class)
public class ProfessorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessorService professorService;

    @MockitoBean
    private ResearchFieldRepository researchFieldRepository;

    @Test
    public void testViewProfile() throws Exception {
        Professor prof = new Professor();
        prof.setId(1L);
        prof.setName("Dr. Alan Turing");
        prof.setEmail("turing@carleton.ca");

        when(professorService.findById(1L)).thenReturn(prof);

        mockMvc.perform(get("/professor/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("professor/profile"))
                .andExpect(model().attributeExists("professor"));
    }
}