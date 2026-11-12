package com.sysc4806.gradadmissions.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.ApplicationStatus;
import com.sysc4806.gradadmissions.repository.ApplicationRepository;

/** Admin-facing use cases for reviewing applications. */
@Service
public class AdminApplicationService {

    private final ApplicationRepository applications;

    public AdminApplicationService(ApplicationRepository applications) {
        this.applications = applications;
    }

    /** All applications that have been submitted (drafts excluded), newest first. */
    @Transactional(readOnly = true)
    public List<Application> submittedApplications() {
        return applications.findByStatusNotOrderBySubmittedAtDesc(ApplicationStatus.DRAFT);
    }

    /** A submitted application by id; drafts are not visible to the admin. */
    @Transactional(readOnly = true)
    public Optional<Application> findSubmittedApplication(Long id) {
        return applications.findById(id)
                .filter(application -> application.getStatus() != ApplicationStatus.DRAFT);
    }
}
