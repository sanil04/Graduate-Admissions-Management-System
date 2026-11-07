package com.sysc4806.gradadmissions.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

	@Column(nullable = false)
	private boolean active;

	public AdmissionCycle(String name, LocalDate applicationDeadline, LocalDate evaluationDeadline) {
		this.name = name;
		this.applicationDeadline = applicationDeadline;
		this.evaluationDeadline = evaluationDeadline;
	}
}
