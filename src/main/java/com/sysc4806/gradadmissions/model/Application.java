package com.sysc4806.gradadmissions.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** An applicant's file for one {@link AdmissionCycle}. */
@Entity
public class Application {

	/** Applicants may name at most this many professors they would like to work with. */
	public static final int MAX_PREFERRED_PROFESSORS = 3;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "applicant_id")
	private Applicant applicant;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "cycle_id")
	private AdmissionCycle cycle;

	@Valid
	@NotNull
	@Embedded
	private PersonalInfo personalInfo;

	@ManyToOne
	@JoinColumn(name = "desired_field_id")
	private ResearchField desiredField;

	/** Stored in the {@code application_preferred_professors} join table. */
	@Size(max = MAX_PREFERRED_PROFESSORS)
	@ManyToMany
	@JoinTable(
			name = "application_preferred_professors",
			joinColumns = @JoinColumn(name = "application_id"),
			inverseJoinColumns = @JoinColumn(name = "professor_id"))
	private Set<Professor> preferredProfessors = new LinkedHashSet<>();

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ApplicationStatus status = ApplicationStatus.DRAFT;

	/** Null until the application is submitted. */
	private LocalDateTime submittedAt;

	protected Application() {
	}

	public Application(Applicant applicant, AdmissionCycle cycle, PersonalInfo personalInfo) {
		this.applicant = applicant;
		this.cycle = cycle;
		this.personalInfo = personalInfo;
	}

	/**
	 * Adds a preferred professor.
	 *
	 * @throws IllegalStateException if the application already lists the maximum number
	 */
	public void addPreferredProfessor(Professor professor) {
		if (!preferredProfessors.contains(professor)
				&& preferredProfessors.size() >= MAX_PREFERRED_PROFESSORS) {
			throw new IllegalStateException(
					"An application can list at most " + MAX_PREFERRED_PROFESSORS + " professors");
		}
		preferredProfessors.add(professor);
	}

	public void removePreferredProfessor(Professor professor) {
		preferredProfessors.remove(professor);
	}

	/**
	 * Moves a draft to SUBMITTED and records the time.
	 *
	 * @throws IllegalStateException if the application is not a draft, or is missing a desired
	 *     field or a preferred professor
	 */
	public void submit(LocalDateTime now) {
		if (status != ApplicationStatus.DRAFT) {
			throw new IllegalStateException("Only a draft can be submitted (status is " + status + ")");
		}
		if (desiredField == null || preferredProfessors.isEmpty()) {
			throw new IllegalStateException(
					"A desired research field and at least one preferred professor are required");
		}
		status = ApplicationStatus.SUBMITTED;
		submittedAt = now;
	}

	public Long getId() {
		return id;
	}

	public Applicant getApplicant() {
		return applicant;
	}

	public AdmissionCycle getCycle() {
		return cycle;
	}

	public PersonalInfo getPersonalInfo() {
		return personalInfo;
	}

	public void setPersonalInfo(PersonalInfo personalInfo) {
		this.personalInfo = personalInfo;
	}

	public ResearchField getDesiredField() {
		return desiredField;
	}

	public void setDesiredField(ResearchField desiredField) {
		this.desiredField = desiredField;
	}

	/** Read-only view; use {@link #addPreferredProfessor} so the limit is enforced. */
	public Set<Professor> getPreferredProfessors() {
		return Collections.unmodifiableSet(preferredProfessors);
	}

	public ApplicationStatus getStatus() {
		return status;
	}

	public void setStatus(ApplicationStatus status) {
		this.status = status;
	}

	public LocalDateTime getSubmittedAt() {
		return submittedAt;
	}
}
