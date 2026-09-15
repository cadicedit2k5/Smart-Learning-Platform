package com.smartlearning.core.document.service.impl;

import com.smartlearning.core.document.service.DocumentDeletionService;
import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.entity.DocumentVersion;
import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;
import com.smartlearning.core.document.messaging.event.DocumentDeletionRequestedEvent;
import com.smartlearning.core.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentDeletionServiceImpl implements DocumentDeletionService {
    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteDocument(UUID courseId, UUID documentId) {
        Document document = documentRepository
                .findByIdAndCourseIdAndDeletedAtIsNull(documentId, courseId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy tài liệu"));

        requireDeletable(document);

        document.setDeletedAt(Instant.now());

        publishDeletionEvent(document);
    }

    private void requireDeletable(Document document) {
        DocumentVersion version = document.getVersion();

        if (version == null) {
            return;
        }

        DocumentProcessingStatus status =
                version.getProcessingStatus();

        if (status != DocumentProcessingStatus.INDEXED
                && status != DocumentProcessingStatus.FAILED) {

            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Tài liệu đang được xử lý, chưa thể xóa"
            );
        }
    }

    private void publishDeletionEvent(
            Document document
    ) {
        eventPublisher.publishEvent(
                new DocumentDeletionRequestedEvent(
                        UUID.randomUUID(),
                        1,
                        Instant.now(),
                        document.getCourse().getId(),
                        document.getId()
                )
        );
    }
}
