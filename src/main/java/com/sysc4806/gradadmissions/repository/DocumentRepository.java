package com.sysc4806.gradadmissions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sysc4806.gradadmissions.model.Document;
import com.sysc4806.gradadmissions.model.DocumentType;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByApplicationIdOrderByTypeAsc(Long applicationId);
    Optional<Document> findByApplicationIdAndType(Long applicationId, DocumentType type);
}
