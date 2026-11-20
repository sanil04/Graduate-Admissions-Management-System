package com.sysc4806.gradadmissions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.Evaluation;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

	/** A professor's assigned applications, newest first, with what their list page shows. */
	@EntityGraph(attributePaths = {"application", "application.desiredField"})
	List<Evaluation> findByProfessorIdOrderByAssignedAtDesc(Long professorId);

	/** Every professor an application was routed to, in routing order, for the admin detail page. */
	@EntityGraph(attributePaths = {"professor"})
	List<Evaluation> findByApplicationIdOrderByAssignedAtAsc(Long applicationId);

	/** Empty when the application was not routed to this professor (used for access checks). */
	Optional<Evaluation> findByApplicationIdAndProfessorId(Long applicationId, Long professorId);

	boolean existsByApplicationIdAndProfessorId(Long applicationId, Long professorId);
}
