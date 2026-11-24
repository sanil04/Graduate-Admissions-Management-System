package com.sysc4806.gradadmissions.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.ApplicationStatus;
import com.sysc4806.gradadmissions.model.Evaluation;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.repository.ApplicationRepository;
import com.sysc4806.gradadmissions.repository.EvaluationRepository;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;

/**
 * Admin-facing use cases for reviewing applications. Rule violations are reported as
 * {@link IllegalArgumentException} (bad input) or {@link IllegalStateException} (a rule of the
 * domain model); the message is safe to show to the user.
 */
@Service
public class AdminApplicationService {

    private final ApplicationRepository applications;
    private final EvaluationRepository evaluations;
    private final ProfessorRepository professors;

    public AdminApplicationService(ApplicationRepository applications, EvaluationRepository evaluations,
                                   ProfessorRepository professors) {
        this.applications = applications;
        this.evaluations = evaluations;
        this.professors = professors;
    }

    /** All applications that have been submitted (drafts excluded), newest first. */
    @Transactional(readOnly = true)
    public List<Application> submittedApplications() {
        return applications.findByStatusNotOrderBySubmittedAtDesc(ApplicationStatus.DRAFT);
    }

    /** A submitted application by id; drafts are not visible to the admin. */
    @Transactional(readOnly = true)
    public Optional<Application> findSubmittedApplication(Long id) {
        return applications.findById(id)
                .filter(application -> application.getStatus() != ApplicationStatus.DRAFT);
    }

    /** The professors an application was routed to, in routing order, with their assessments. */
    @Transactional(readOnly = true)
    public List<Evaluation> evaluationsFor(Long applicationId) {
        return evaluations.findByApplicationIdOrderByAssignedAtAsc(applicationId);
    }

    /** Every professor, by name, for the routing form. */
    @Transactional(readOnly = true)
    public List<Professor> allProfessors() {
        return professors.findAll(Sort.by("name"));
    }

    /**
     * Routes a submitted application to professors: one pending {@link Evaluation} per professor,
     * and the application goes UNDER_REVIEW. While it is under review, it can be routed to more
     * professors, but never to the same professor twice.
     *
     * @return the evaluations created, one per professor
     */
    @Transactional
    public List<Evaluation> route(Long applicationId, List<Long> professorIds, LocalDateTime now) {
        Application application = submittedApplication(applicationId);
        if (professorIds == null || professorIds.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one professor");
        }
        List<Evaluation> created = new ArrayList<>();
        for (Long professorId : new LinkedHashSet<>(professorIds)) {
            Professor professor = professors.findById(professorId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown professor"));
            if (evaluations.existsByApplicationIdAndProfessorId(applicationId, professorId)) {
                throw new IllegalStateException("This application was already routed to " + professor.getName());
            }
            created.add(new Evaluation(application, professor, now));
        }
        application.startReview();
        return evaluations.saveAll(created);
    }

    /** Screens out a submitted application that hasn't been routed to any professor. */
    @Transactional
    public void screenOut(Long applicationId) {
        submittedApplication(applicationId).screenOut();
    }

    private Application submittedApplication(Long id) {
        return findSubmittedApplication(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown application"));
    }
}
