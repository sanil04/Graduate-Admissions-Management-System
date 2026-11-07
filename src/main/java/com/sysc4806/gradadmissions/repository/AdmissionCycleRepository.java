package com.sysc4806.gradadmissions.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.AdmissionCycle;

public interface AdmissionCycleRepository extends JpaRepository<AdmissionCycle, Long> {

	/** The cycle new applications go into. Only one cycle should be active at a time. */
	Optional<AdmissionCycle> findFirstByActiveTrue();
}
