package com.sysc4806.gradadmissions.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Equal by name (unique), so fields behave correctly in sets even before they are saved. */
@Entity
@Getter
@Setter
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
