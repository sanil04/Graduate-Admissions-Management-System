package com.sysc4806.gradadmissions.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.model.Evaluation;
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
}