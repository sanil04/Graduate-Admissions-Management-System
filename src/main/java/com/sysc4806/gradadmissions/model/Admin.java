package com.sysc4806.gradadmissions.model;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class Admin extends User {

	public Admin(String name, String email) {
		super(name, email);
	}
}
