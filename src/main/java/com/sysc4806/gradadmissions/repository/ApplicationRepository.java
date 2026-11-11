package com.sysc4806.gradadmissions.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

	List<Application> findByStatusOrderBySubmittedAtAsc(ApplicationStatus status);
}
