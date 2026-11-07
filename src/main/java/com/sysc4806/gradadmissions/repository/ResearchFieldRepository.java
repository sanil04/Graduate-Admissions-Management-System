package com.sysc4806.gradadmissions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.ResearchField;

public interface ResearchFieldRepository extends JpaRepository<ResearchField, Long> {

	Optional<ResearchField> findByNameIgnoreCase(String name);

	List<ResearchField> findAllByOrderByNameAsc();
}
