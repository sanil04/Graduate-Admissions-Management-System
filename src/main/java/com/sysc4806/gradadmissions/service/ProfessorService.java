package com.sysc4806.gradadmissions.service;

import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfessorService {

    @Autowired
    private ProfessorRepository professorRepository;

    public Professor findById(Long id) {
        return professorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid professor Id:" + id));
    }

    public Professor save(Professor professor) {
        return professorRepository.save(professor);
    }
}