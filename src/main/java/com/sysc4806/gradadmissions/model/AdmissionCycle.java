package com.sysc4806.gradadmissions.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * One admission round, e.g. "Fall 2027", with its deadlines. Only one cycle is active at a time;
 * new applications go into the active cycle.
 */
@Entity
public class AdmissionCycle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(nullable = false, unique = true)
	private String name;

	/** Last day applicants can submit. */
	@NotNull
	@Column(nullable = false)
	private LocalDate applicationDeadline;

	/** Last day professors can enter their evaluations. */
	@NotNull
	@Column(nullable = false)
	private LocalDate evaluationDeadline;

	@Column(nullable = false)
	private boolean active;

	protected AdmissionCycle() {
	}

	public AdmissionCycle(String name, LocalDate applicationDeadline, LocalDate evaluationDeadline) {
		this.name = name;
		this.applicationDeadline = applicationDeadline;
		this.evaluationDeadline = evaluationDeadline;
	}

	/** True if applications can still be submitted on the given date (the deadline day counts). */
	public boolean isAcceptingApplications(LocalDate today) {
		return active && !today.isAfter(applicationDeadline);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDate getApplicationDeadline() {
		return applicationDeadline;
	}

	public void setApplicationDeadline(LocalDate applicationDeadline) {
		this.applicationDeadline = applicationDeadline;
	}

	public LocalDate getEvaluationDeadline() {
		return evaluationDeadline;
	}

	public void setEvaluationDeadline(LocalDate evaluationDeadline) {
		this.evaluationDeadline = evaluationDeadline;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
