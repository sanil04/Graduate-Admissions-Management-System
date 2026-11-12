package com.sysc4806.gradadmissions.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;
import com.sysc4806.gradadmissions.web.dto.ProfessorFormDto;

/** Professor-facing use cases: viewing and editing a profile. */
@Service
public class ProfessorService {

    private final ProfessorRepository professors;
    private final ResearchFieldRepository fields;
    private final UserRepository users;

    public ProfessorService(ProfessorRepository professors, ResearchFieldRepository fields, UserRepository users) {
        this.professors = professors;
        this.fields = fields;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Professor> findAll() {
        return professors.findAll(Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public Optional<Professor> findById(Long id) {
        return professors.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ResearchField> researchFields() {
        return fields.findAll(Sort.by("name"));
    }

    /** Replaces the professor's name, email and research fields with the form's values. */
    @Transactional
    public Professor update(Long id, ProfessorFormDto form) {
        Professor professor = professors.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown professor"));
        // Emails are unique across all roles.
        if (users.findByEmail(form.getEmail()).filter(user -> !user.getId().equals(id)).isPresent()) {
            throw new IllegalArgumentException("That email address belongs to another account");
        }
        professor.setName(form.getName());
        professor.setEmail(form.getEmail());
        professor.getResearchFields().clear();
        professor.getResearchFields().addAll(fields.findAllById(form.getResearchFieldIds()));
        return professors.save(professor);
    }
}
