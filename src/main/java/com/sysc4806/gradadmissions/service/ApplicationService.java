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
import com.sysc4806.gradadmissions.model.User;
import com.sysc4806.gradadmissions.repository.AdmissionCycleRepository;
import com.sysc4806.gradadmissions.repository.ApplicantRepository;
import com.sysc4806.gradadmissions.repository.ApplicationRepository;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;

/**
 * Applicant-facing use cases. Rule violations are reported as
 * {@link IllegalArgumentException} (bad input) or {@link IllegalStateException}
 * (a rule of the domain model, e.g. deadline passed); the message is safe to show to the user.
 */
@Service
public class ApplicationService {

    private final ApplicationRepository applications;
    private final ApplicantRepository applicants;
    private final UserRepository users;
    private final AdmissionCycleRepository cycles;
    private final ResearchFieldRepository fields;
    private final ProfessorRepository professors;

    public ApplicationService(ApplicationRepository applications, ApplicantRepository applicants,
                              UserRepository users, AdmissionCycleRepository cycles, ResearchFieldRepository fields,
                              ProfessorRepository professors) {
        this.applications = applications;
        this.applicants = applicants;
        this.users = users;
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

    @Transactional(readOnly = true)
    public Optional<Application> findApplication(Long id) {
        return applications.findById(id);
    }

    /** Creates the application in the active cycle and submits it at {@code now}. */
    @Transactional
    public Application submit(PersonalInfo personalInfo, Long fieldId, List<Long> professorIds,
                              LocalDateTime now) {
        AdmissionCycle cycle = cycles.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Applications are not open right now"));
        ResearchField field = fields.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown research field"));

        Application application = new Application(findOrCreateApplicant(personalInfo), cycle, personalInfo);
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

    /** Emails are unique across all roles, so an existing applicant is reused. */
    private Applicant findOrCreateApplicant(PersonalInfo info) {
        Optional<User> existing = users.findByEmail(info.getEmail());
        if (existing.isEmpty()) {
            return applicants.save(
                    new Applicant(info.getFirstName() + " " + info.getLastName(), info.getEmail()));
        }
        if (existing.get() instanceof Applicant applicant) {
            return applicant;
        }
        throw new IllegalArgumentException("That email address belongs to another account");
    }
}