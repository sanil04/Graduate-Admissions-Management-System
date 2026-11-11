package com.sysc4806.gradadmissions.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Keep the name fixed because it determines equality and hash codes in sets. */
@Entity
@Getter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ResearchField {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@EqualsAndHashCode.Include
	@NotBlank
	@Column(nullable = false, unique = true)
	private String name;

	public ResearchField(String name) {
		this.name = name;
	}
}
