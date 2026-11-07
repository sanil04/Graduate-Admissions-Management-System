package com.sysc4806.gradadmissions.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

/** A research area, e.g. "Machine Learning". Names are unique. */
@Entity
public class ResearchField {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(nullable = false, unique = true)
	private String name;

	protected ResearchField() {
	}

	public ResearchField(String name) {
		this.name = name;
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

	/** Equal by name (the natural key), so fields behave correctly in sets even before saving. */
	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof ResearchField other && Objects.equals(name, other.name));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}

	@Override
	public String toString() {
		return name;
	}
}
