package com.smartlearning.core.document.service;

import java.util.UUID;

public interface DocumentDeletionService {
    void deleteDocument(UUID courseId, UUID documentId);
}
