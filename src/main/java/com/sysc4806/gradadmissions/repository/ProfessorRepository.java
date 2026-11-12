package com.sysc4806.gradadmissions.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.Professor;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {

    List<Professor> findByResearchFieldsIdOrderByNameAsc(Long researchFieldId);
}