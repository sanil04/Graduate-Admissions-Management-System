package com.sysc4806.gradadmissions.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.User;

/** Polymorphic access to all users, whatever their role (used for login in Sprint 2). */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);
}
