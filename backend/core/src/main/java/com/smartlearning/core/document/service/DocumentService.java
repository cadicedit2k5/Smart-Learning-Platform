package com.smartlearning.core.document.service;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.document.dto.request.DocumentCreateRequest;
import com.smartlearning.core.document.dto.request.DocumentFilterRequest;
import com.smartlearning.core.document.dto.request.DocumentUpdateRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.storage.dto.StoredFile;

import java.util.UUID;

public interface DocumentService {
    DocumentResponse handleCreateDocument(UUID courseId, UUID currentUserId,
                                          DocumentCreateRequest request);

    PagingResponse<DocumentResponse> handleGetAll(UUID courseId, UUID currentUserId,
            DocumentFilterRequest filter);

    DocumentResponse handleGetDocument(UUID courseId, UUID documentId, UUID currentUserId);

    DocumentResponse handleUpdateDocument(
            UUID courseId,
            UUID documentId,
            UUID currentUserId,
            DocumentUpdateRequest request
    );

    StoredFile handleDownloadDocument(UUID courseId, UUID documentId, UUID currentUserId);

    void handleDeleteDocument(UUID courseId, UUID documentId, UUID currentUserId);
}
