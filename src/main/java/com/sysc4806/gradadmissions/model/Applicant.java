package com.sysc4806.gradadmissions.model;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class Applicant extends User {

	public Applicant(String name, String email) {
		super(name, email);
	}
}
