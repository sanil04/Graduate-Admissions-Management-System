package com.sysc4806.gradadmissions.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.Document;
import com.sysc4806.gradadmissions.model.DocumentType;
import com.sysc4806.gradadmissions.model.Admin;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.User;
import com.sysc4806.gradadmissions.repository.ApplicationRepository;
import com.sysc4806.gradadmissions.repository.DocumentRepository;
import com.sysc4806.gradadmissions.repository.EvaluationRepository;

@Service
public class DocumentService {

    private final DocumentRepository documents;
    private final ApplicationRepository applications;
    private final EvaluationRepository evaluations;

    public DocumentService(DocumentRepository documents, ApplicationRepository applications, EvaluationRepository evaluations) {
        this.documents = documents;
        this.applications = applications;
        this.evaluations = evaluations;
    }

    @Transactional(readOnly = true)
    public Document download(Long id, User user) {
        Document document = documents.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Application application = document.getApplication();
        boolean allowed = user instanceof Admin
                || user instanceof Applicant && application.getApplicant().getId().equals(user.getId())
                || user instanceof Professor && evaluations.existsByApplicationIdAndProfessorId(application.getId(), user.getId());
        if (!allowed) throw new AccessDeniedException("You cannot download documents for this application");
        return document;
    }

    @Transactional(readOnly = true)
    public Application ownedApplication(Long id, Applicant applicant) {
        Application application = applications.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!application.getApplicant().getId().equals(applicant.getId())) {
            throw new AccessDeniedException("You can only upload documents to your own application");
        }
        return application;
    }

    @Transactional(readOnly = true)
    public List<Document> uploadedDocuments(Long applicationId, Applicant applicant) {
        ownedApplication(applicationId, applicant);
        return documents.findByApplicationIdOrderByTypeAsc(applicationId);
    }

    @Transactional
    public Document upload(Long applicationId, Applicant applicant, DocumentType type,
                           MultipartFile file, LocalDateTime now) throws IOException {
        Application application = ownedApplication(applicationId, applicant);
        if (!application.isDocumentsEditable()) {
            throw new IllegalStateException("Documents can only be changed while the application is a draft");
        }
        application.getCycle().requireApplicationOpen(now.toLocalDate());
        if (type == null) throw new IllegalArgumentException("Choose a document type");
        if (file == null || file.isEmpty() || file.getSize() > Document.MAX_SIZE) {
            throw new IllegalArgumentException("Choose a PDF no larger than 5 MB");
        }
        byte[] bytes = file.getBytes();
        if (!"application/pdf".equalsIgnoreCase(file.getContentType()) || bytes.length < 5
                || bytes.length > Document.MAX_SIZE
                || !"%PDF-".equals(new String(bytes, 0, 5, StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("The file must be a PDF");
        }
        String fileName = file.getOriginalFilename();
        fileName = fileName == null ? "document.pdf" : fileName.replace('\\', '/');
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "");
        if (fileName.isBlank()) fileName = "document.pdf";
        if (fileName.length() > 255) throw new IllegalArgumentException("The file name is too long");
        Document document = documents.findByApplicationIdAndType(applicationId, type)
                .orElseGet(() -> new Document(application, type));
        document.replace(fileName, bytes, now);
        return documents.save(document);
    }
}
