package com.smartlearning.core.document.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseChapter;
import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.repository.CourseChapterRepository;
import com.smartlearning.core.course.repository.CourseTopicRepository;
import com.smartlearning.core.course.utils.CourseUtils;
import com.smartlearning.core.document.dto.request.DocumentCreateRequest;
import com.smartlearning.core.document.dto.request.DocumentFilterRequest;
import com.smartlearning.core.document.dto.request.DocumentUpdateRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.entity.DocumentProcessingJob;
import com.smartlearning.core.document.entity.DocumentVersion;
import com.smartlearning.core.document.entity.enums.DocumentLifecycleStatus;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import com.smartlearning.core.document.mapper.DocumentMapper;
import com.smartlearning.core.document.messaging.event.DocumentIngestionRequestedEvent;
import com.smartlearning.core.document.messaging.publisher.DocumentDeletionEventPublisher;
import com.smartlearning.core.document.messaging.publisher.DocumentIngestionEventPublisher;
import com.smartlearning.core.document.repository.DocumentProcessingJobRepository;
import com.smartlearning.core.document.repository.DocumentRepository;
import com.smartlearning.core.document.repository.DocumentVersionRepository;
import com.smartlearning.storage.config.MinioProperties;
import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.dto.StoredFile;
import com.smartlearning.storage.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.DOCUMENT_ID;
import static com.smartlearning.core.support.CoreTestData.OWNER_ID;
import static com.smartlearning.core.support.CoreTestData.PROCESSING_JOB_ID;
import static com.smartlearning.core.support.CoreTestData.VERSION_ID;
import static com.smartlearning.core.support.CoreTestData.course;
import static com.smartlearning.core.support.CoreTestData.document;
import static com.smartlearning.core.support.CoreTestData.documentResponse;
import static com.smartlearning.core.support.CoreTestData.documentVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private CourseUtils courseUtils;
    @Mock
    private CourseAccessPolicy courseAccessPolicy;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentVersionRepository versionRepository;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private DocumentProcessingJobRepository processingJobRepository;
    @Mock
    private DocumentIngestionEventPublisher eventPublisher;
    @Mock
    private CourseChapterRepository chapterRepository;
    @Mock
    private CourseTopicRepository topicRepository;
    @Mock
    private DocumentDeletionEventPublisher documentDeletionEventPublisher;

    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        MinioProperties minioProperties = new MinioProperties(
                "http://localhost:9000",
                "test-access-key",
                "test-secret-key",
                "smart-learning-test",
                Duration.ofMinutes(15)
        );
        documentService = new DocumentServiceImpl(
                courseUtils,
                courseAccessPolicy,
                fileStorageService,
                transactionTemplate,
                documentRepository,
                minioProperties,
                versionRepository,
                documentMapper,
                processingJobRepository,
                eventPublisher,
                chapterRepository,
                topicRepository,
                documentDeletionEventPublisher
        );
    }

    @Test
    void handleGetAll_checksCourseAndMembershipThenReturnsMappedPage() {
        DocumentFilterRequest filter = mock(DocumentFilterRequest.class);
        @SuppressWarnings("unchecked")
        Specification<Document> filterSpecification = mock(Specification.class);
        Pageable pageable = PageRequest.of(1, 2);
        Document first = document();
        Document second = document();
        second.setId(java.util.UUID.randomUUID());
        second.setTitle("Second document");
        DocumentResponse firstResponse = documentResponse();
        DocumentResponse secondResponse = new DocumentResponse(
                second.getId(), COURSE_ID, null, null, second.getTitle(), second.getDescription(),
                second.getLifecycleStatus(), second.getUploadedBy(), null, second.getCreatedAt(), second.getUpdatedAt()
        );
        Page<Document> page = new PageImpl<>(List.of(first, second), pageable, 5);
        when(filter.specification()).thenReturn(filterSpecification);
        when(filter.pageable()).thenReturn(pageable);
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(documentMapper.toResponse(first)).thenReturn(firstResponse);
        when(documentMapper.toResponse(second)).thenReturn(secondResponse);

        PagingResponse<DocumentResponse> result = documentService.handleGetAll(COURSE_ID, OWNER_ID, filter);

        assertThat(result.getContent()).containsExactly(firstResponse, secondResponse);
        assertThat(result.getPageable().getPage()).isEqualTo(2);
        assertThat(result.getPageable().getSize()).isEqualTo(2);
        assertThat(result.getPageable().getTotalElements()).isEqualTo(5);
        assertThat(result.getPageable().getTotalPages()).isEqualTo(3);
        InOrder order = inOrder(courseUtils, courseAccessPolicy, documentRepository);
        order.verify(courseUtils).requireCourse(COURSE_ID);
        order.verify(courseAccessPolicy).requireActiveMember(COURSE_ID, OWNER_ID);
        order.verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void handleCreateDocument_runsUploadTransactionAndPublishInOrder() {
        Course course = course();
        DocumentCreateRequest request = request();
        FileUploadResponse upload = uploadResponse(request);
        DocumentResponse expected = documentResponse();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course);
        when(fileStorageService.upload(request.getFile(), documentFolder())).thenReturn(upload);
        executeTransactionCallback();
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(invocation -> {
            Document saved = invocation.getArgument(0);
            saved.setId(DOCUMENT_ID);
            return saved;
        });
        when(versionRepository.saveAndFlush(any(DocumentVersion.class))).thenAnswer(invocation -> {
            DocumentVersion saved = invocation.getArgument(0);
            saved.setId(VERSION_ID);
            return saved;
        });
        when(processingJobRepository.saveAndFlush(any(DocumentProcessingJob.class))).thenAnswer(invocation -> {
            DocumentProcessingJob saved = invocation.getArgument(0);
            saved.setId(PROCESSING_JOB_ID);
            return saved;
        });
        when(documentMapper.toResponse(any(Document.class))).thenReturn(expected);

        DocumentResponse result = documentService.handleCreateDocument(COURSE_ID, OWNER_ID, request);

        assertThat(result).isSameAs(expected);
        InOrder outerOrder = inOrder(courseUtils, courseAccessPolicy, fileStorageService, transactionTemplate, eventPublisher);
        outerOrder.verify(courseUtils).requireCourse(COURSE_ID);
        outerOrder.verify(courseAccessPolicy).requireOwner(COURSE_ID, OWNER_ID);
        outerOrder.verify(fileStorageService).upload(request.getFile(), documentFolder());
        outerOrder.verify(transactionTemplate).execute(any(TransactionCallback.class));
        outerOrder.verify(courseUtils).requireCourse(COURSE_ID);
        outerOrder.verify(courseAccessPolicy).requireOwner(COURSE_ID, OWNER_ID);

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<DocumentVersion> versionCaptor = ArgumentCaptor.forClass(DocumentVersion.class);
        ArgumentCaptor<DocumentProcessingJob> jobCaptor = ArgumentCaptor.forClass(DocumentProcessingJob.class);
        InOrder persistenceOrder = inOrder(documentRepository, versionRepository, processingJobRepository, documentMapper);
        persistenceOrder.verify(documentRepository).saveAndFlush(documentCaptor.capture());
        persistenceOrder.verify(versionRepository).saveAndFlush(versionCaptor.capture());
        persistenceOrder.verify(processingJobRepository).saveAndFlush(jobCaptor.capture());
        persistenceOrder.verify(documentRepository).flush();
        persistenceOrder.verify(documentMapper).toResponse(documentCaptor.getValue());

        Document savedDocument = documentCaptor.getValue();
        DocumentVersion savedVersion = versionCaptor.getValue();
        DocumentProcessingJob savedJob = jobCaptor.getValue();
        assertThat(savedDocument.getCourse()).isSameAs(course);
        assertThat(savedDocument.getTitle()).isEqualTo("Document title");
        assertThat(savedDocument.getDescription()).isEqualTo("Document description");
        assertThat(savedDocument.getLifecycleStatus()).isEqualTo(DocumentLifecycleStatus.ACTIVE);
        assertThat(savedDocument.getUploadedBy()).isEqualTo(OWNER_ID);
        assertThat(savedDocument.getVersion()).isSameAs(savedVersion);
        assertThat(savedVersion.getDocument()).isSameAs(savedDocument);
        assertThat(savedVersion.getVersionNumber()).isEqualTo(1);
        assertThat(savedVersion.getFileName()).isEqualTo(upload.originalFileName());
        assertThat(savedVersion.getFileSize()).isEqualTo(upload.size());
        assertThat(savedVersion.getMimeType()).isEqualTo(upload.contentType());
        assertThat(savedVersion.getStorageBucket()).isEqualTo("smart-learning-test");
        assertThat(savedVersion.getStorageKey()).isEqualTo(upload.objectName());
        assertThat(savedVersion.getProcessingStatus()).isEqualTo(DocumentProcessingStatus.QUEUED);
        assertThat(savedVersion.getUploadedBy()).isEqualTo(OWNER_ID);
        assertThat(savedJob.getDocumentVersion()).isSameAs(savedVersion);

        ArgumentCaptor<DocumentIngestionRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(DocumentIngestionRequestedEvent.class);
        outerOrder.verify(eventPublisher).publish(eventCaptor.capture());
        DocumentIngestionRequestedEvent event = eventCaptor.getValue();
        assertThat(event.processingJobId()).isEqualTo(PROCESSING_JOB_ID);
        assertThat(event.courseId()).isEqualTo(COURSE_ID);
        assertThat(event.documentId()).isEqualTo(DOCUMENT_ID);
        assertThat(event.documentVersionId()).isEqualTo(VERSION_ID);
        assertThat(event.storageBucket()).isEqualTo("smart-learning-test");
        assertThat(event.storageKey()).isEqualTo(upload.objectName());
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void handleCreateDocument_deletesUploadedObjectWhenTransactionFails() {
        DocumentCreateRequest request = request();
        FileUploadResponse upload = uploadResponse(request);
        RuntimeException persistenceFailure = new RuntimeException("database failure");
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(fileStorageService.upload(request.getFile(), documentFolder())).thenReturn(upload);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenThrow(persistenceFailure);

        assertThatThrownBy(() -> documentService.handleCreateDocument(COURSE_ID, OWNER_ID, request))
                .isSameAs(persistenceFailure);

        verify(fileStorageService).delete(upload.objectName());
        verifyNoInteractions(documentRepository, versionRepository, processingJobRepository, documentMapper, eventPublisher);
    }

    @Test
    void handleCreateDocument_preservesOriginalFailureAndSuppressesCleanupFailure() {
        DocumentCreateRequest request = request();
        FileUploadResponse upload = uploadResponse(request);
        RuntimeException persistenceFailure = new RuntimeException("database failure");
        RuntimeException cleanupFailure = new RuntimeException("storage cleanup failure");
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(fileStorageService.upload(request.getFile(), documentFolder())).thenReturn(upload);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenThrow(persistenceFailure);
        org.mockito.Mockito.doThrow(cleanupFailure).when(fileStorageService).delete(upload.objectName());

        assertThatThrownBy(() -> documentService.handleCreateDocument(COURSE_ID, OWNER_ID, request))
                .isSameAs(persistenceFailure)
                .satisfies(exception -> assertThat(exception.getSuppressed()).containsExactly(cleanupFailure));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void handleGetDocument_checksMembershipAndReturnsDocument() {
        Document existing = document();
        DocumentResponse expected = documentResponse();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(documentRepository.findByIdAndCourseIdAndDeletedAtIsNull(DOCUMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(existing));
        when(documentMapper.toResponse(existing)).thenReturn(expected);

        assertThat(documentService.handleGetDocument(COURSE_ID, DOCUMENT_ID, OWNER_ID))
                .isSameAs(expected);

        verify(courseAccessPolicy).requireActiveMember(COURSE_ID, OWNER_ID);
    }

    @Test
    void handleUpdateDocument_updatesMetadataAfterTeachingAccessCheck() {
        Document existing = document();
        DocumentUpdateRequest request = new DocumentUpdateRequest(
                "  Tiêu đề mới  ",
                "Mô tả mới",
                DocumentLifecycleStatus.ARCHIVED,
                null,
                null
        );
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(documentRepository.findByIdAndCourseIdAndDeletedAtIsNull(DOCUMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(existing));
        when(documentMapper.toResponse(existing)).thenAnswer(invocation -> documentResponse());

        documentService.handleUpdateDocument(COURSE_ID, DOCUMENT_ID, OWNER_ID, request);

        assertThat(existing.getTitle()).isEqualTo("Tiêu đề mới");
        assertThat(existing.getDescription()).isEqualTo("Mô tả mới");
        assertThat(existing.getLifecycleStatus()).isEqualTo(DocumentLifecycleStatus.ARCHIVED);
        verify(courseAccessPolicy).requireOwner(COURSE_ID, OWNER_ID);
    }

    @Test
    void handleUpdateDocument_placesDocumentOnlyInTopicWithActiveChapter() {
        UUID chapterId = UUID.fromString("90000000-0000-0000-0000-000000000001");
        UUID topicId = UUID.fromString("91000000-0000-0000-0000-000000000001");
        CourseChapter chapter = new CourseChapter();
        chapter.setId(chapterId);
        chapter.setCourse(course());
        CourseTopic topic = new CourseTopic();
        topic.setId(topicId);
        topic.setChapter(chapter);
        Document existing = document();
        DocumentUpdateRequest request = new DocumentUpdateRequest(null, null, null, null, topicId);

        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(documentRepository.findByIdAndCourseIdAndDeletedAtIsNull(DOCUMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(existing));
        when(topicRepository.findByIdAndChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(
                topicId,
                COURSE_ID
        )).thenReturn(Optional.of(topic));
        when(documentMapper.toResponse(existing)).thenReturn(documentResponse());

        documentService.handleUpdateDocument(COURSE_ID, DOCUMENT_ID, OWNER_ID, request);

        assertThat(existing.getChapter()).isSameAs(chapter);
        assertThat(existing.getTopic()).isSameAs(topic);
        verify(topicRepository).findByIdAndChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(
                topicId,
                COURSE_ID
        );
    }

    @Test
    void handleDownloadDocument_returnsStoredFileForCurrentVersion() {
        Document existing = document();
        DocumentVersion version = documentVersion();
        existing.setVersion(version);
        StoredFile expected = new StoredFile(
                new ByteArrayInputStream("file".getBytes(StandardCharsets.UTF_8)),
                "application/pdf",
                4,
                "lesson.pdf"
        );
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(documentRepository.findByIdAndCourseIdAndDeletedAtIsNull(DOCUMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(existing));
        when(fileStorageService.download(version.getStorageKey())).thenReturn(expected);

        assertThat(documentService.handleDownloadDocument(COURSE_ID, DOCUMENT_ID, OWNER_ID))
                .isSameAs(expected);
        verify(courseAccessPolicy).requireActiveMember(COURSE_ID, OWNER_ID);
    }

    @Test
    void handleDeleteDocument_softDeletesAndArchivesDocument() {
        Document existing = document();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(documentRepository.findByIdAndCourseIdAndDeletedAtIsNull(DOCUMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(existing));

        Instant beforeCall = Instant.now();
        documentService.handleDeleteDocument(COURSE_ID, DOCUMENT_ID, OWNER_ID);
        Instant afterCall = Instant.now();

        assertThat(existing.getLifecycleStatus()).isEqualTo(DocumentLifecycleStatus.ARCHIVED);
        assertThat(existing.getDeletedAt()).isBetween(beforeCall, afterCall);
        verify(courseAccessPolicy).requireOwner(COURSE_ID, OWNER_ID);
    }

    private void executeTransactionCallback() {
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    private static DocumentCreateRequest request() {
        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setTitle("  Document title  ");
        request.setDescription("Document description");
        request.setFile(new MockMultipartFile(
                "file", "lesson.pdf", "application/pdf",
                "fake-pdf-content".getBytes(StandardCharsets.UTF_8)
        ));
        return request;
    }

    private static FileUploadResponse uploadResponse(DocumentCreateRequest request) {
        return new FileUploadResponse(
                documentFolder() + "/object-lesson.pdf",
                request.getFile().getOriginalFilename(),
                request.getFile().getContentType(),
                request.getFile().getSize()
        );
    }

    private static String documentFolder() {
        return "core/courses/" + COURSE_ID + "/documents";
    }
}
