package com.smartlearning.core.document.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.utils.CourseUtils;
import com.smartlearning.core.document.dto.request.DocumentCreateRequest;
import com.smartlearning.core.document.dto.request.DocumentFilterRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.entity.DocumentProcessingJob;
import com.smartlearning.core.document.entity.DocumentVersion;
import com.smartlearning.core.document.entity.enums.DocumentLifecycleStatus;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import com.smartlearning.core.document.mapper.DocumentMapper;
import com.smartlearning.core.document.messaging.event.DocumentIngestionRequestedEvent;
import com.smartlearning.core.document.messaging.publisher.DocumentIngestionEventPublisher;
import com.smartlearning.core.document.repository.DocumentProcessingJobRepository;
import com.smartlearning.core.document.repository.DocumentRepository;
import com.smartlearning.core.document.repository.DocumentVersionRepository;
import com.smartlearning.core.document.repository.specification.DocumentSpecifications;
import com.smartlearning.core.document.service.DocumentService;
import com.smartlearning.storage.config.MinioProperties;
import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
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
    public DocumentResponse handleCreateDocument(UUID courseId, UUID currentUserId, DocumentCreateRequest request) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireTeachingMember(courseId,currentUserId);

        String folder = "core/courses/" + courseId + "/documents";

        FileUploadResponse uploadedFile = fileStorageService.upload(request.getFile(),folder);

        DocumentCreationResult result;
        try { result = transactionTemplate.execute(status -> {
                    Course course = courseUtils.requireCourse(courseId);

                    // Kiểm tra lại trong transaction.
                    courseAccessPolicy.requireTeachingMember(
                            courseId,
                            currentUserId
                    );

                    Document document = new Document();
                    document.setCourse(course);
                    document.setTitle(request.getTitle().trim());
                    document.setDescription(request.getDescription());
                    document.setLifecycleStatus(DocumentLifecycleStatus.ACTIVE);
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
