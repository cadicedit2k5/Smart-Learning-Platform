package com.smartlearning.core.document.service.impl;

import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.utils.CourseUtils;
import com.smartlearning.core.document.dto.request.DocumentCreateRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.entity.DocumentVersion;
import com.smartlearning.core.document.entity.enums.DocumentLifecycleStatus;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import com.smartlearning.core.document.mapper.DocumentMapper;
import com.smartlearning.core.document.repository.DocumentRepository;
import com.smartlearning.core.document.repository.DocumentVersionRepository;
import com.smartlearning.core.document.service.DocumentService;
import com.smartlearning.storage.config.MinioProperties;
import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Override
    public DocumentResponse createDocument(UUID courseId, UUID currentUserId, DocumentCreateRequest request) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireTeachingMember(courseId,currentUserId);

        String folder = "core/courses/" + courseId + "/documents";

        FileUploadResponse uploadedFile = fileStorageService.upload(request.getFile(),folder);

        try {
            DocumentResponse response =
                transactionTemplate.execute(status -> {
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
                    version.setProcessingStatus(DocumentProcessingStatus.UPLOADED);
                    version.setUploadedBy(currentUserId);

                    versionRepository.saveAndFlush(version);

                    document.setVersion(version);
                    documentRepository.flush();

                    return documentMapper.toResponse(document);
                });

            return Objects.requireNonNull(response);
        } catch (RuntimeException exception) {
            cleanupUploadedObject(uploadedFile.objectName(), exception);
            throw exception;
        }
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
}
