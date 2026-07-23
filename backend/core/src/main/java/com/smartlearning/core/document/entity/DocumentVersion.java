package com.smartlearning.core.document.entity;

import com.smartlearning.common.entity.BaseEntity;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "document_version",
        schema = "core",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_version_number",
                        columnNames = {"document_id", "version_number"}
                ),
                @UniqueConstraint(
                        name = "uk_document_storage_object",
                        columnNames = {"storage_bucket", "storage_key"}
                )
        }
)
public class DocumentVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "mime_type", length = 150)
    private String mimeType;

    @Column(name = "storage_bucket", nullable = false, length = 100)
    private String storageBucket;

    @Column(name = "storage_key", nullable = false, columnDefinition = "TEXT")
    private String storageKey;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private DocumentProcessingStatus processingStatus =
            DocumentProcessingStatus.UPLOADED;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;
}
