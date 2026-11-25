package com.sysc4806.gradadmissions.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import com.sysc4806.gradadmissions.model.*;
import com.sysc4806.gradadmissions.repository.DocumentRepository;

@DataJpaTest
@Import(DocumentService.class)
class DocumentServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 12, 0);
    @Autowired private DocumentService service;
    @Autowired private DocumentRepository documents;
    @Autowired private TestEntityManager em;
    private Applicant applicant;
    private Application application;

    @BeforeEach
    void setUp() {
        applicant = em.persist(new Applicant("Ada", "ada@example.com"));
        AdmissionCycle cycle = em.persist(new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1)));
        application = em.persist(new Application(applicant, cycle, new PersonalInfo("Ada", "Doe", "ada@example.com", null, null)));
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII));
    }

    @ParameterizedTest
    @EnumSource(DocumentType.class)
    void pdfBytesAndMetadataSurviveReload(DocumentType type) throws Exception {
        Document saved = service.upload(application.getId(), applicant, type, pdf("cv.pdf"), NOW);
        em.flush();
        em.clear();
        Document loaded = documents.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getBytes()).isEqualTo(pdf("cv.pdf").getBytes());
        assertThat(loaded.getSize()).isEqualTo(pdf("cv.pdf").getSize());
        assertThat(loaded.getFileName()).isEqualTo("cv.pdf");
        assertThat(loaded.getContentType()).isEqualTo("application/pdf");
        assertThat(loaded.getType()).isEqualTo(type);
        assertThat(loaded.getUploadedAt()).isEqualTo(NOW);
        assertThat(loaded.getApplication().getId()).isEqualTo(application.getId());
    }

    @Test
    void reuploadReplacesOnlyThatTypeAndKeepsItsId() throws Exception {
        Long id = service.upload(application.getId(), applicant, DocumentType.CV, pdf("old.pdf"), NOW).getId();
        service.upload(application.getId(), applicant, DocumentType.DIPLOMA, pdf("diploma.pdf"), NOW);
        service.upload(application.getId(), applicant, DocumentType.CV, pdf("new.pdf"), NOW.plusHours(1));
        em.flush();
        em.clear();
        assertThat(documents.count()).isEqualTo(2);
        Document replaced = documents.findById(id).orElseThrow();
        assertThat(replaced.getFileName()).isEqualTo("new.pdf");
        assertThat(replaced.getUploadedAt()).isEqualTo(NOW.plusHours(1));
    }

    @Test
    void invalidUploadsDoNotEraseThePreviousFile() throws Exception {
        service.upload(application.getId(), applicant, DocumentType.CV, pdf("original.pdf"), NOW);
        MockMultipartFile[] invalid = {
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]),
                new MockMultipartFile("file", "text.pdf", "application/pdf", "plain text".getBytes()),
                new MockMultipartFile("file", "wrong.txt", "text/plain", pdf("cv.pdf").getBytes()),
                new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[Document.MAX_SIZE + 1])
        };
        for (MockMultipartFile file : invalid) {
            assertThatThrownBy(() -> service.upload(application.getId(), applicant, DocumentType.CV, file, NOW))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(documents.findByApplicationIdAndType(application.getId(), DocumentType.CV)).get()
                .extracting(Document::getFileName).isEqualTo("original.pdf");
    }

    @Test
    void fiveMegabytesIsAllowedAndFilePathsAreRemoved() throws Exception {
        byte[] bytes = new byte[Document.MAX_SIZE];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 5);
        service.upload(application.getId(), applicant, DocumentType.CV,
                new MockMultipartFile("file", "C:\\fakepath\\cv.pdf", "application/pdf", bytes), NOW);
        em.flush();
        em.clear();
        Document loaded = documents.findByApplicationIdAndType(application.getId(), DocumentType.CV).orElseThrow();
        assertThat(loaded.getSize()).isEqualTo(Document.MAX_SIZE);
        assertThat(loaded.getFileName()).isEqualTo("cv.pdf");
    }

    @Test
    void anotherApplicantCannotListOrReplaceDocuments() {
        Applicant other = em.persist(new Applicant("Other", "other@example.com"));
        assertThatThrownBy(() -> service.uploadedDocuments(application.getId(), other)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.upload(application.getId(), other, DocumentType.CV, pdf("cv.pdf"), NOW))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(documents.count()).isZero();
    }

    @Test
    void databaseRejectsTwoDocumentsOfTheSameTypeForAnApplication() throws Exception {
        service.upload(application.getId(), applicant, DocumentType.CV, pdf("first.pdf"), NOW);
        Document duplicate = new Document(application, DocumentType.CV);
        duplicate.replace("second.pdf", pdf("second.pdf").getBytes(), NOW);
        assertThatThrownBy(() -> documents.saveAndFlush(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
