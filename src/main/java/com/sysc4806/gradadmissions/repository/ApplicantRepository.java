package com.sysc4806.gradadmissions.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.Applicant;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {

	Optional<Applicant> findByEmail(String email);
}
