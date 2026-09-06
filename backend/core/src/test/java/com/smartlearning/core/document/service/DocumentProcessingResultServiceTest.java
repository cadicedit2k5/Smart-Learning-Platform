package com.smartlearning.core.document.service;

import com.smartlearning.core.document.entity.DocumentProcessingJob;
import com.smartlearning.core.document.entity.DocumentVersion;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import com.smartlearning.core.document.entity.enums.ProcessingJobStatus;
import com.smartlearning.core.document.messaging.event.DocumentIngestionCompletedEvent;
import com.smartlearning.core.document.messaging.event.DocumentIngestionFailedEvent;
import com.smartlearning.core.document.repository.DocumentProcessingJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.smartlearning.core.support.CoreTestData.PROCESSING_JOB_ID;
import static com.smartlearning.core.support.CoreTestData.VERSION_ID;
import static com.smartlearning.core.support.CoreTestData.completedEvent;
import static com.smartlearning.core.support.CoreTestData.failedEvent;
import static com.smartlearning.core.support.CoreTestData.processingJob;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingResultServiceTest {

    @Mock
    private DocumentProcessingJobRepository processingJobRepository;
    @InjectMocks
    private DocumentProcessingResultService resultService;

    @Test
    void handleCompleted_updatesJobAndVersionAtomicallyThroughManagedEntities() {
        DocumentProcessingJob job = processingJob(ProcessingJobStatus.PENDING);
        DocumentVersion version = job.getDocumentVersion();
        job.setErrorMessage("old job error");
        version.setErrorMessage("old version error");
        when(processingJobRepository.findById(PROCESSING_JOB_ID)).thenReturn(Optional.of(job));

        Instant beforeCall = Instant.now();
        resultService.handleCompleted(completedEvent(PROCESSING_JOB_ID, VERSION_ID));
        Instant afterCall = Instant.now();

        assertThat(job.getStatus()).isEqualTo(ProcessingJobStatus.COMPLETED);
        assertThat(job.getFinishedAt()).isBetween(beforeCall, afterCall);
        assertThat(job.getErrorMessage()).isNull();
        assertThat(version.getProcessingStatus()).isEqualTo(DocumentProcessingStatus.INDEXED);
        assertThat(version.getIndexedAt()).isEqualTo(job.getFinishedAt());
        assertThat(version.getErrorMessage()).isNull();
        verify(processingJobRepository, never()).save(job);
    }

    @Test
    void handleCompleted_isIdempotentWhenJobAlreadyCompleted() {
        DocumentProcessingJob job = processingJob(ProcessingJobStatus.COMPLETED);
        Instant originalFinishedAt = Instant.parse("2026-08-01T00:00:00Z");
        job.setFinishedAt(originalFinishedAt);
        job.getDocumentVersion().setProcessingStatus(DocumentProcessingStatus.INDEXED);
        when(processingJobRepository.findById(PROCESSING_JOB_ID)).thenReturn(Optional.of(job));

        resultService.handleCompleted(completedEvent(PROCESSING_JOB_ID, VERSION_ID));

        assertThat(job.getFinishedAt()).isEqualTo(originalFinishedAt);
        assertThat(job.getDocumentVersion().getProcessingStatus()).isEqualTo(DocumentProcessingStatus.INDEXED);
    }

    @Test
    void handleCompleted_rejectsMissingJobAndMismatchedVersion() {
        DocumentIngestionCompletedEvent event = completedEvent(PROCESSING_JOB_ID, VERSION_ID);
        when(processingJobRepository.findById(PROCESSING_JOB_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> resultService.handleCompleted(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(PROCESSING_JOB_ID.toString());

        DocumentProcessingJob job = processingJob(ProcessingJobStatus.PENDING);
        when(processingJobRepository.findById(PROCESSING_JOB_ID)).thenReturn(Optional.of(job));
        assertThatThrownBy(() -> resultService.handleCompleted(completedEvent(PROCESSING_JOB_ID, UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void handleFailed_updatesJobAndVersionWithSameError() {
        DocumentProcessingJob job = processingJob(ProcessingJobStatus.PENDING);
        when(processingJobRepository.findById(PROCESSING_JOB_ID)).thenReturn(Optional.of(job));
        DocumentIngestionFailedEvent event = failedEvent(PROCESSING_JOB_ID, VERSION_ID);

        Instant beforeCall = Instant.now();
        resultService.handleFailed(event);
        Instant afterCall = Instant.now();

        assertThat(job.getStatus()).isEqualTo(ProcessingJobStatus.FAILED);
        assertThat(job.getFinishedAt()).isBetween(beforeCall, afterCall);
        assertThat(job.getErrorMessage()).isEqualTo(event.errorMessage());
        assertThat(job.getDocumentVersion().getProcessingStatus()).isEqualTo(DocumentProcessingStatus.FAILED);
        assertThat(job.getDocumentVersion().getErrorMessage()).isEqualTo(event.errorMessage());
        verify(processingJobRepository, never()).save(job);
    }

    @Test
    void handleFailed_isIdempotentWhenJobAlreadyFailed() {
        DocumentProcessingJob job = processingJob(ProcessingJobStatus.FAILED);
        Instant originalFinishedAt = Instant.parse("2026-08-01T00:00:00Z");
        job.setFinishedAt(originalFinishedAt);
        job.setErrorMessage("original error");
        job.getDocumentVersion().setErrorMessage("original error");
        when(processingJobRepository.findById(PROCESSING_JOB_ID)).thenReturn(Optional.of(job));

        resultService.handleFailed(failedEvent(PROCESSING_JOB_ID, VERSION_ID));

        assertThat(job.getFinishedAt()).isEqualTo(originalFinishedAt);
        assertThat(job.getErrorMessage()).isEqualTo("original error");
        assertThat(job.getDocumentVersion().getErrorMessage()).isEqualTo("original error");
    }

    @Test
    void handleFailed_rejectsMissingJobAndMismatchedVersion() {
        DocumentIngestionFailedEvent event = failedEvent(PROCESSING_JOB_ID, VERSION_ID);
        when(processingJobRepository.findById(PROCESSING_JOB_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> resultService.handleFailed(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(PROCESSING_JOB_ID.toString());

        DocumentProcessingJob job = processingJob(ProcessingJobStatus.PENDING);
        when(processingJobRepository.findById(PROCESSING_JOB_ID)).thenReturn(Optional.of(job));
        assertThatThrownBy(() -> resultService.handleFailed(failedEvent(PROCESSING_JOB_ID, UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match");
    }
}
