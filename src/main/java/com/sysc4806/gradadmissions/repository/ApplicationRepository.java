package com.sysc4806.gradadmissions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;


import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

	List<Application> findByStatusOrderBySubmittedAtAsc(ApplicationStatus status);
	Optional<Application> findByApplicantIdAndCycleId(Long applicantId, Long cycleId);


	/**
	 * Every application not in {@code status}, newest submission first. The entity graph fetches
	 * the field and preferred professors in the same query, avoiding one extra query per row (N+1).
	 */
	@EntityGraph(attributePaths = {"desiredField", "preferredProfessors"})
	List<Application> findByStatusNotOrderBySubmittedAtDesc(ApplicationStatus status);
}