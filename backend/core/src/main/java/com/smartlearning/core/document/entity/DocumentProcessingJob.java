package com.smartlearning.core.document.entity;

import com.smartlearning.common.entity.BaseEntity;
import com.smartlearning.core.document.entity.enums.ProcessingJobStatus;
import com.smartlearning.core.document.entity.enums.ProcessingJobType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "document_processing_job",
        schema = "core",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_version_job_type",
                        columnNames = {"document_version_id", "job_type"}
                )
        }
)
public class DocumentProcessingJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private ProcessingJobType jobType =
            ProcessingJobType.EXTRACT_AND_INDEX;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProcessingJobStatus status =
            ProcessingJobStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}