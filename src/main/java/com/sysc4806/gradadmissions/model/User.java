package com.sysc4806.gradadmissions.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Base class for every person who uses the system.
 *
 * <p>Mapped with {@link InheritanceType#JOINED}: the shared columns (id, name, email) live in the
 * {@code users} table, and each subclass ({@link Applicant}, {@link Professor}, {@link Admin}) has
 * its own table holding only its extra columns, joined to {@code users} by primary key. We chose
 * JOINED over SINGLE_TABLE so subclass-specific columns can be NOT NULL and the schema stays
 * normalized, and over TABLE_PER_CLASS so a single query on {@code users} can find anyone by email
 * (needed for login in Sprint 2).
 *
 * <p>The table is named {@code users} because {@code user} is a reserved word in H2 and PostgreSQL.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
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

	protected User() {
	}

	protected User(String name, String email) {
		this.name = name;
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
