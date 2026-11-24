package com.sysc4806.gradadmissions.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.model.Evaluation;
import com.sysc4806.gradadmissions.model.Recommendation;
import com.sysc4806.gradadmissions.repository.EvaluationRepository;

/**
 * What a professor can see of the applications routed to them. An application belongs to a
 * professor only through an {@link Evaluation}, so every lookup goes through the evaluations.
 */
@Service
public class ProfessorApplicationService {

    private final EvaluationRepository evaluations;

    public ProfessorApplicationService(EvaluationRepository evaluations) {
        this.evaluations = evaluations;
    }

    /** The professor's assigned applications, most recently routed first. */
    @Transactional(readOnly = true)
    public List<Evaluation> assignedTo(Long professorId) {
        return evaluations.findByProfessorIdOrderByAssignedAtDesc(professorId);
    }

    /**
     * The evaluation of an application routed to this professor. Empty when the application
     * doesn't exist or was routed to someone else, so callers can't tell the two apart.
     */
    @Transactional(readOnly = true)
    public Optional<Evaluation> findAssigned(Long applicationId, Long professorId) {
        return evaluations.findByApplicationIdAndProfessorId(applicationId, professorId);
    }

    /**
     * Records the professor's assessment of an application routed to them, through
     * {@link Evaluation#submitAssessment}. The application's status is not touched: only the admin
     * changes it.
     *
     * @throws NoSuchElementException   if the application doesn't exist or wasn't routed to this professor
     * @throws IllegalArgumentException if no recommendation was chosen or the comments are too long
     * @throws IllegalStateException    if the assessment was already submitted
     */
    @Transactional
    public Evaluation submitAssessment(Long applicationId, Long professorId, Recommendation recommendation,
                                       String comments, LocalDateTime now) {
        Evaluation evaluation = evaluations.findByApplicationIdAndProfessorId(applicationId, professorId)
                .orElseThrow(() -> new NoSuchElementException("This application was not routed to you"));
        evaluation.submitAssessment(recommendation, comments, now);
        return evaluations.save(evaluation);
    }
}