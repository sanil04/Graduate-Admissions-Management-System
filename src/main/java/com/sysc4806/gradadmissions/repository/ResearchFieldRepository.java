package com.sysc4806.gradadmissions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.ResearchField;
import java.util.Optional;

public interface ResearchFieldRepository extends JpaRepository<ResearchField, Long> {
    Optional<ResearchField> findByName(String name);
}
