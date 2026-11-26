package com.sysc4806.gradadmissions.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;
import com.sysc4806.gradadmissions.web.dto.AdminProfessorFormDto;
import com.sysc4806.gradadmissions.web.dto.ProfessorFormDto;

/** Professor-facing use cases: viewing and editing a profile. */
@Service
public class ProfessorService {

    private final ProfessorRepository professors;
    private final ResearchFieldRepository fields;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public ProfessorService(ProfessorRepository professors, ResearchFieldRepository fields, UserRepository users,
                            PasswordEncoder passwordEncoder) {
        this.professors = professors;
        this.fields = fields;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
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

    /** Message shown when a profile would be left without any research field. */
    public static final String NO_RESEARCH_FIELDS_MESSAGE = "Select at least one research field";

    /** Thrown when create/update would leave a professor with no research fields. */
    public static class NoResearchFieldsException extends IllegalArgumentException {
        public NoResearchFieldsException() {
            super(NO_RESEARCH_FIELDS_MESSAGE);
        }
    }

    /** Creates a professor with a temporary password so they can log in. */
    @Transactional
    public Professor create(AdminProfessorFormDto form) {
        if (users.findByEmail(form.getEmail()).isPresent()) {
            throw new IllegalArgumentException("That email address belongs to another account");
        }
        if (form.getResearchFieldIds() == null || form.getResearchFieldIds().isEmpty()) {
            throw new NoResearchFieldsException();
        }
        List<ResearchField> selected = fields.findAllById(form.getResearchFieldIds());
        if (selected.isEmpty()) {
            throw new NoResearchFieldsException();
        }
        Professor professor = new Professor(form.getName(), form.getEmail());
        professor.setPasswordHash(passwordEncoder.encode(form.getTemporaryPassword()));
        professor.getResearchFields().addAll(selected);
        return professors.save(professor);
    }

    /**
     * Replaces the professor's name, email and research fields with the form's values.
     *
     * @throws NoResearchFieldsException if no (existing) research field is selected
     * @throws IllegalArgumentException if the professor is unknown or the email belongs to another account
     */
    @Transactional
    public Professor update(Long id, ProfessorFormDto form) {
        Professor professor = professors.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown professor"));
        if (form.getResearchFieldIds() == null || form.getResearchFieldIds().isEmpty()) {
            throw new NoResearchFieldsException();
        }
        // Emails are unique across all roles.
        if (users.findByEmail(form.getEmail()).filter(user -> !user.getId().equals(id)).isPresent()) {
            throw new IllegalArgumentException("That email address belongs to another account");
        }
        List<ResearchField> selected = fields.findAllById(form.getResearchFieldIds());
        if (selected.isEmpty()) {
            throw new NoResearchFieldsException();
        }
        professor.setName(form.getName());
        professor.setEmail(form.getEmail());
        professor.getResearchFields().clear();
        professor.getResearchFields().addAll(selected);
        return professors.save(professor);
    }
}
