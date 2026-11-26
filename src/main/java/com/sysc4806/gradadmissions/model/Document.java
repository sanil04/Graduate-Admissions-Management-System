package com.sysc4806.gradadmissions.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_document_application_type", columnNames = {"application_id", "type"}))
public class Document {

    public static final int MAX_SIZE = 5 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private Application application;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false, length = MAX_SIZE)
    private byte[] bytes;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    public Document(Application application, DocumentType type) {
        this.application = application;
        this.type = type;
    }

    public void replace(String fileName, byte[] bytes, LocalDateTime uploadedAt) {
        this.fileName = fileName;
        this.contentType = "application/pdf";
        this.size = bytes.length;
        this.bytes = bytes;
        this.uploadedAt = uploadedAt;
    }
}
