package com.sysc4806.gradadmissions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {}
