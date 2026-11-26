package com.sysc4806.gradadmissions.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);
}
