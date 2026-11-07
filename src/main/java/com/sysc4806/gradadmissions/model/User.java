package com.sysc4806.gradadmissions.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Base class for every user. Mapped with JOINED inheritance: shared columns live in {@code users}
 * and each role has its own table joined by id. Chosen over SINGLE_TABLE so role-only columns can
 * be NOT NULL, and over TABLE_PER_CLASS so one query on {@code users} finds anyone by email (login).
 * The table is {@code users} because {@code user} is reserved in H2 and PostgreSQL.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public abstract class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(nullable = false)
	private String name;

	@NotBlank
	@Email
	@Column(nullable = false, unique = true)
	private String email;

	protected User(String name, String email) {
		this.name = name;
		this.email = email;
	}
}
