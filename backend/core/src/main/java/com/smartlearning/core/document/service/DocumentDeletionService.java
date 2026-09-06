package com.smartlearning.core.document.service;

import java.util.UUID;

public interface DocumentDeletionService {
    void deleteDocument(UUID courseId, UUID documentId);

    void deleteByScope(UUID courseId, UUID chapterId, UUID topicId);
}
