package com.sysc4806.gradadmissions.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.model.AdmissionCycle;
import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.PersonalInfo;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;
import com.sysc4806.gradadmissions.repository.AdmissionCycleRepository;
import com.sysc4806.gradadmissions.repository.ApplicationRepository;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;

/**
 * Applicant-facing use cases. Rule violations are reported as
 * {@link IllegalArgumentException} (bad input) or {@link IllegalStateException}
 * (a rule of the domain model, e.g. deadline passed); the message is safe to show to the user.
 */
@Service
public class ApplicationService {

    private final ApplicationRepository applications;
    private final AdmissionCycleRepository cycles;
    private final ResearchFieldRepository fields;
    private final ProfessorRepository professors;

    public ApplicationService(ApplicationRepository applications, AdmissionCycleRepository cycles,
                              ResearchFieldRepository fields, ProfessorRepository professors) {
        this.applications = applications;
        this.cycles = cycles;
        this.fields = fields;
        this.professors = professors;
    }

    @Transactional(readOnly = true)
    public List<ResearchField> researchFields() {
        return fields.findAll(Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public List<Professor> professorsForField(Long fieldId) {
        return professors.findByResearchFieldsIdOrderByNameAsc(fieldId);
    }

    /** The application, only if it belongs to {@code applicant}; applicants never see each other's. */
    @Transactional(readOnly = true)
    public Optional<Application> findApplicationOf(Long id, Applicant applicant) {
        return applications.findById(id)
                .filter(application -> application.getApplicant().getId().equals(applicant.getId()));
    }

    /**
     * Creates {@code applicant}'s application in the active cycle and submits it at {@code now}.
     * The contact email is always the account's, whatever {@code personalInfo} says.
     */
    @Transactional
    public Application submit(Applicant applicant, PersonalInfo personalInfo, Long fieldId,
                              List<Long> professorIds, LocalDateTime now) {
        AdmissionCycle cycle = cycles.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Applications are not open right now"));
        ResearchField field = fields.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown research field"));

        personalInfo.setEmail(applicant.getEmail());
        Application application = new Application(applicant, cycle, personalInfo);
        application.setDesiredField(field);
        List<Long> ids = professorIds == null ? List.of() : professorIds;
        for (Long professorId : new LinkedHashSet<>(ids)) {
            Professor professor = professors.findById(professorId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown professor"));
            if (!professor.getResearchFields().contains(field)) {
                throw new IllegalArgumentException(
                        professor.getName() + " does not work in " + field.getName());
            }
            application.addPreferredProfessor(professor);
        }
        application.submit(now);
        return applications.save(application);
    }
}