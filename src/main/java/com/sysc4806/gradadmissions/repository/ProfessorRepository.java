package com.sysc4806.gradadmissions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {

	Optional<Professor> findByEmail(String email);

	/** Professors who list the given field, e.g. to suggest supervisors on the application form. */
	List<Professor> findByResearchFieldsContaining(ResearchField field);

	List<Professor> findAllByOrderByNameAsc();
}
