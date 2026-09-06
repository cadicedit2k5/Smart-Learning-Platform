package com.smartlearning.core.document.service;

import com.smartlearning.core.document.entity.DocumentProcessingJob;
import com.smartlearning.core.document.entity.DocumentVersion;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import com.smartlearning.core.document.entity.enums.ProcessingJobStatus;
import com.smartlearning.core.document.messaging.event.DocumentIngestionCompletedEvent;
import com.smartlearning.core.document.messaging.event.DocumentIngestionFailedEvent;
import com.smartlearning.core.document.repository.DocumentProcessingJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DocumentProcessingResultService {

    private final DocumentProcessingJobRepository processingJobRepository;

    @Transactional
    public void handleCompleted(DocumentIngestionCompletedEvent event) {

        DocumentProcessingJob job = processingJobRepository.findById(event.processingJobId())
                .orElseThrow(() -> new IllegalStateException(
                        "Document processing job not found: " + event.processingJobId()));

        DocumentVersion version = job.getDocumentVersion();

        if (!version.getId().equals(event.documentVersionId())) {
            throw new IllegalStateException("Document version does not match processing job");
        }

        if (job.getStatus() == ProcessingJobStatus.COMPLETED) {
            return;
        }
        Instant now = Instant.now();

        job.setStatus(ProcessingJobStatus.COMPLETED);
        job.setFinishedAt(now);
        job.setErrorMessage(null);

        version.setProcessingStatus(DocumentProcessingStatus.INDEXED);
        version.setIndexedAt(now);
        version.setErrorMessage(null);
    }

    @Transactional
    public void handleFailed(DocumentIngestionFailedEvent event) {

        DocumentProcessingJob job = processingJobRepository.findById(event.processingJobId())
                        .orElseThrow(() -> new IllegalStateException(
                                        "Document processing job not found: " + event.processingJobId()));

        DocumentVersion version = job.getDocumentVersion();

        if (!version.getId().equals(event.documentVersionId())) {
            throw new IllegalStateException(
                    "Document version does not match processing job"
            );
        }
        if (job.getStatus() == ProcessingJobStatus.FAILED) {
            return;
        }

        Instant now = Instant.now();

        job.setStatus(ProcessingJobStatus.FAILED);
        job.setFinishedAt(now);
        job.setErrorMessage(event.errorMessage());

        version.setProcessingStatus(DocumentProcessingStatus.FAILED);
        version.setErrorMessage(event.errorMessage());
    }

}