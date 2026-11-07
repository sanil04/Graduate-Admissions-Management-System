package com.sysc4806.gradadmissions.model;

import jakarta.persistence.Entity;

/** A prospective graduate student who submits {@link Application}s. */
@Entity
public class Applicant extends User {

	protected Applicant() {
	}

	public Applicant(String name, String email) {
		super(name, email);
	}
}
