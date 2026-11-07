package com.sysc4806.gradadmissions.model;

import jakarta.persistence.Entity;

/** A department administrator who screens applications and makes the final decision. */
@Entity
public class Admin extends User {

	protected Admin() {
	}

	public Admin(String name, String email) {
		super(name, email);
	}
}
