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
}
