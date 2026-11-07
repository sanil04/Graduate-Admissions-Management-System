package com.sysc4806.gradadmissions.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/** A faculty member who lists research fields and evaluates applicants routed to them. */
@Entity
public class Professor extends User {

	/** Unidirectional many-to-many, stored in the {@code professor_research_fields} join table. */
	@ManyToMany
	@JoinTable(
			name = "professor_research_fields",
			joinColumns = @JoinColumn(name = "professor_id"),
			inverseJoinColumns = @JoinColumn(name = "research_field_id"))
	private Set<ResearchField> researchFields = new HashSet<>();

	protected Professor() {
	}

	public Professor(String name, String email) {
		super(name, email);
	}

	public Set<ResearchField> getResearchFields() {
		return researchFields;
	}

	public void addResearchField(ResearchField field) {
		researchFields.add(field);
	}

	public void removeResearchField(ResearchField field) {
		researchFields.remove(field);
	}
}
