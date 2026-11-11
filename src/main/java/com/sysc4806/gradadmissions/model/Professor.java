package com.sysc4806.gradadmissions.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Professor extends User {

	@ManyToMany
	@JoinTable(name = "professor_research_fields",
			joinColumns = @JoinColumn(name = "professor_id"),
			inverseJoinColumns = @JoinColumn(name = "research_field_id"))
	private Set<ResearchField> researchFields = new HashSet<>();

	public Professor(String name, String email) {
		super(name, email);
	}
}
