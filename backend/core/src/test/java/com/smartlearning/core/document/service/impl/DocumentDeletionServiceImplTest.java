package com.smartlearning.core.document.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.entity.CourseChapter;
import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.entity.DocumentVersion;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import com.smartlearning.core.document.messaging.event.DocumentDeletionRequestedEvent;
import com.smartlearning.core.document.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.smartlearning.core.support.CoreTestData.CHAPTER_ID;
import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.DOCUMENT_ID;
import static com.smartlearning.core.support.CoreTestData.TOPIC_ID;
import static com.smartlearning.core.support.CoreTestData.chapter;
import static com.smartlearning.core.support.CoreTestData.document;
import static com.smartlearning.core.support.CoreTestData.documentVersion;
import static com.smartlearning.core.support.CoreTestData.topic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentDeletionServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private DocumentDeletionServiceImpl deletionService;

    @ParameterizedTest(name = "deletes document in final processing state {0}")
    @EnumSource(value = DocumentProcessingStatus.class, names = {"INDEXED", "FAILED"})
    void deleteDocument_marksFinalDocumentDeletedAndPublishesEvent(
            DocumentProcessingStatus status
    ) {
        Document document = documentWithStatus(status);
        when(documentRepository.findByIdAndCourseIdAndDeletedAtIsNull(DOCUMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(document));

        Instant beforeCall = Instant.now();
        deletionService.deleteDocument(COURSE_ID, DOCUMENT_ID);
        Instant afterCall = Instant.now();

        assertThat(document.getDeletedAt()).isBetween(beforeCall, afterCall);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        DocumentDeletionRequestedEvent event =
                (DocumentDeletionRequestedEvent) eventCaptor.getValue();
        assertThat(event.courseId()).isEqualTo(COURSE_ID);
        assertThat(event.documentId()).isEqualTo(DOCUMENT_ID);
        assertThat(event.occurredAt()).isBetween(beforeCall, afterCall);
    }

    @ParameterizedTest(name = "blocks document in processing state {0}")
    @EnumSource(value = DocumentProcessingStatus.class, names = {"UPLOADED", "QUEUED"})
    void deleteDocument_rejectsDocumentStillBeingProcessed(
            DocumentProcessingStatus status
    ) {
        Document document = documentWithStatus(status);
        when(documentRepository.findByIdAndCourseIdAndDeletedAtIsNull(DOCUMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(document));

        assertThatThrownBy(() -> deletionService.deleteDocument(COURSE_ID, DOCUMENT_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.DATA_CONFLICT);

        assertThat(document.getDeletedAt()).isNull();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deleteDocument_returnsNotFoundForMissingDocument() {
        when(documentRepository.findByIdAndCourseIdAndDeletedAtIsNull(DOCUMENT_ID, COURSE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deletionService.deleteDocument(COURSE_ID, DOCUMENT_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);

        verifyNoInteractions(eventPublisher);
    }

    private static Document documentWithStatus(DocumentProcessingStatus status) {
        Document document = document();
        DocumentVersion version = documentVersion();
        version.setDocument(document);
        version.setProcessingStatus(status);
        document.setVersion(version);
        return document;
    }
}
