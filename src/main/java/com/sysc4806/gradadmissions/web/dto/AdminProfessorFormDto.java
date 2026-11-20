package com.sysc4806.gradadmissions.web.dto;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

// What the admin "create professor" form binds to
@Getter
@Setter
public class AdminProfessorFormDto {

	@NotBlank(message = "Name cannot be blank")
	private String name;

	@NotBlank(message = "Email cannot be blank")
	@Email(message = "Invalid email format")
	private String email;

	@NotBlank(message = "Temporary password cannot be blank")
	private String temporaryPassword;

	@NotEmpty(message = "Choose at least one research field")
	private Set<Long> researchFieldIds = new LinkedHashSet<>();
}