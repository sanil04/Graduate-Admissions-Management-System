package com.sysc4806.gradadmissions.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One admission round, e.g. "Fall 2027". Only one cycle is active at a time. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class AdmissionCycle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(nullable = false, unique = true)
	private String name;

	@NotNull
	@Column(nullable = false)
	private LocalDate applicationDeadline;

	@NotNull
	@Column(nullable = false)
	private LocalDate evaluationDeadline;

	// Inactive cycles use NULL so only TRUE occupies the unique slot.
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Column(unique = true)
	private Boolean active;

	public boolean isActive() {
		return Boolean.TRUE.equals(active);
	}

	public void setActive(boolean active) {
		this.active = active ? Boolean.TRUE : null;
	}

	public AdmissionCycle(String name, LocalDate applicationDeadline, LocalDate evaluationDeadline) {
		this.name = name;
		this.applicationDeadline = applicationDeadline;
		this.evaluationDeadline = evaluationDeadline;
	}

	/** Applications are accepted throughout the application deadline's date. */
	public boolean isApplicationOpenOn(LocalDate date) {
		return !date.isAfter(applicationDeadline);
	}

	/** Assessments are accepted, and applications can be routed, throughout the evaluation deadline's date. */
	public boolean isEvaluationOpenOn(LocalDate date) {
		return !date.isAfter(evaluationDeadline);
	}

	/** @throws IllegalStateException if the application deadline has passed on {@code date} */
	public void requireApplicationOpen(LocalDate date) {
		if (!isApplicationOpenOn(date)) {
			throw new IllegalStateException("The application deadline has passed");
		}
	}

	/** @throws IllegalStateException if the evaluation deadline has passed on {@code date} */
	public void requireEvaluationOpen(LocalDate date) {
		if (!isEvaluationOpenOn(date)) {
			throw new IllegalStateException("The evaluation deadline has passed");
		}
	}
}
