package com.sysc4806.gradadmissions.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Contact details on an application. Embedded: stored as columns of the application table. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalInfo {

	@NotBlank
	@Column(nullable = false)
	private String firstName;

	@NotBlank
	@Column(nullable = false)
	private String lastName;

	@NotBlank
	@Email
	@Column(nullable = false)
	private String email;

	private String phoneNumber;

	private String citizenship;
}
