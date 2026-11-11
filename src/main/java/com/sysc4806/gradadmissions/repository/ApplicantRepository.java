package com.sysc4806.gradadmissions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.Applicant;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {}
