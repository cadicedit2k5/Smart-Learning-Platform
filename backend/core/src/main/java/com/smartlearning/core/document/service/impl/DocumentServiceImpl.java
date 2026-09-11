package com.smartlearning.core.document.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.utils.CourseUtils;
import com.smartlearning.core.document.dto.request.DocumentCreateRequest;
import com.smartlearning.core.document.dto.request.DocumentFilterRequest;
import com.smartlearning.core.document.dto.request.DocumentUpdateRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.entity.DocumentProcessingJob;
import com.smartlearning.core.document.entity.DocumentVersion;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import com.smartlearning.core.document.mapper.DocumentMapper;
import com.smartlearning.core.document.messaging.event.DocumentDeletionRequestedEvent;
import com.smartlearning.core.document.messaging.event.DocumentIngestionRequestedEvent;
import com.smartlearning.core.document.messaging.publisher.DocumentDeletionEventPublisher;
import com.smartlearning.core.document.messaging.publisher.DocumentIngestionEventPublisher;
import com.smartlearning.core.document.repository.DocumentProcessingJobRepository;
import com.smartlearning.core.document.repository.DocumentRepository;
import com.smartlearning.core.document.repository.DocumentVersionRepository;
import com.smartlearning.core.document.repository.specification.DocumentSpecifications;
import com.smartlearning.core.document.service.DocumentDeletionService;
import com.smartlearning.core.document.service.DocumentService;
import com.smartlearning.core.document.utils.DocumentUtils;
import com.smartlearning.storage.config.MinioProperties;
import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.dto.StoredFile;
import com.smartlearning.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final CourseUtils courseUtils;
    private final CourseAccessPolicy courseAccessPolicy;
    private final FileStorageService fileStorageService;
    private final TransactionTemplate transactionTemplate;
    private final DocumentRepository documentRepository;
    private final MinioProperties minioProperties;
    private final DocumentVersionRepository versionRepository;
    private final DocumentMapper documentMapper;
    private final DocumentProcessingJobRepository documentProcessingJobRepository;
    private final DocumentIngestionEventPublisher documentIngestionEventPublisher;
    private final DocumentDeletionEventPublisher documentDeletionEventPublisher;
    private final DocumentDeletionService documentDeletionService;
    private final DocumentUtils documentUtils;

    @Override
    @Transactional(readOnly = true)
    public PagingResponse<DocumentResponse> handleGetAll(UUID courseId, UUID currentUserId,
            DocumentFilterRequest filter) {
        courseUtils.requireCourse(courseId);

        courseAccessPolicy.requireActiveMember(courseId, currentUserId);

        Specification<Document> specification = Specification.allOf(
                DocumentSpecifications.courseId(courseId),
                filter.specification()
        );

        Page<DocumentResponse> documents = documentRepository.findAll(specification, filter.pageable())
                .map(documentMapper::toResponse);

        return PagingResponse.from(documents);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse handleGetDocument(UUID courseId, UUID documentId, UUID currentUserId) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireActiveMember(courseId, currentUserId);
        return documentMapper.toResponse(requireDocument(courseId, documentId));
    }

    @Override
    public DocumentResponse handleCreateDocument(UUID courseId, UUID currentUserId, DocumentCreateRequest request) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);

        String folder = "core/courses/" + courseId + "/documents";

        FileUploadResponse uploadedFile = fileStorageService.upload(request.getFile(),folder);

        DocumentCreationResult result;
        try { result = transactionTemplate.execute(status -> {
                    Course course = courseUtils.requireCourse(courseId);

                    // Kiểm tra lại trong transaction.
                    courseAccessPolicy.requireOwner(
                            courseId,
                            currentUserId
                    );

                    Document document = new Document();
                    document.setCourse(course);
                    document.setTitle(request.getTitle().trim());
                    document.setDescription(request.getDescription());
                    document.setUploadedBy(currentUserId);

                    documentRepository.saveAndFlush(document);

                    DocumentVersion version = new DocumentVersion();

                    version.setDocument(document);
                    version.setVersionNumber(1);
                    version.setFileName(
                            uploadedFile.originalFileName()
                    );
                    version.setFileSize(
                            uploadedFile.size()
                    );
                    version.setMimeType(uploadedFile.contentType());
                    version.setStorageBucket(minioProperties.bucket());
                    version.setStorageKey(uploadedFile.objectName());
                    version.setChecksumSha256(null);
                    version.setProcessingStatus(DocumentProcessingStatus.QUEUED);
                    version.setUploadedBy(currentUserId);

                    versionRepository.saveAndFlush(version);

                    // Tạo processing job
                    DocumentProcessingJob processingJob = new DocumentProcessingJob();

                    processingJob.setDocumentVersion(version);

                    documentProcessingJobRepository.saveAndFlush(processingJob);

                    // Tạo event
                    UUID eventId = UUID.randomUUID();

                    DocumentIngestionRequestedEvent event = new DocumentIngestionRequestedEvent(
                                    eventId,
                                    1,
                                    Instant.now(),
                                    processingJob.getId(),
                                    courseId,
                                    document.getId(),
                                    version.getId(),
                                    version.getStorageBucket(),
                                    version.getStorageKey(),
                                    version.getFileName(),
                                    version.getMimeType());

                    document.setVersion(version);
                    documentRepository.flush();

                    return new DocumentCreationResult(
                            documentMapper.toResponse(document),
                            event
                    );
                });
        } catch (RuntimeException exception) {
            cleanupUploadedObject(uploadedFile.objectName(), exception);
            throw exception;
        }

        result = Objects.requireNonNull(result);

        documentIngestionEventPublisher.publish(result.event());

        return result.response();
    }

    @Override
    @Transactional
    public DocumentResponse handleUpdateDocument(
            UUID courseId,
            UUID documentId,
            UUID currentUserId,
            DocumentUpdateRequest request
    ) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);
        Document document = requireDocument(courseId, documentId);

        if (request.title() != null) {
            document.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            document.setDescription(request.description());
        }

        return documentMapper.toResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile handleDownloadDocument(UUID courseId, UUID documentId, UUID currentUserId) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireActiveMember(courseId, currentUserId);
        Document document = requireDocument(courseId, documentId);

        return documentUtils.downloadDocument(document);
    }

    @Override
    @Transactional
    public void handleDeleteDocument(UUID courseId, UUID documentId, UUID currentUserId) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);
        documentDeletionService.deleteDocument(courseId, documentId);
    }

    private Document requireDocument(UUID courseId, UUID documentId) {
        return documentRepository.findByIdAndCourseIdAndDeletedAtIsNull(documentId, courseId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy tài liệu"
                ));
    }

    private void cleanupUploadedObject(
            String objectName,
            RuntimeException originalException
    ) {
        try {
            fileStorageService.delete(objectName);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);

            log.error(
                    "Không thể xóa object: {}",
                    objectName,
                    cleanupException
            );
        }
    }

    // dùng cho create document
    private record DocumentCreationResult(
            DocumentResponse response,
            DocumentIngestionRequestedEvent event
    ) {
    }

}
