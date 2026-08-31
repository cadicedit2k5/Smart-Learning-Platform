package com.smartlearning.core.document.service;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.document.dto.request.AdminDocumentFilterRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;

import java.util.UUID;

public interface DocumentAdminService {
    PagingResponse<DocumentResponse> handleGetAllForAdmin(AdminDocumentFilterRequest filter);

    DocumentResponse handleGetDocumentForAdmin(UUID documentId);

    void handleDeleteDocumentForAdmin(UUID documentId);
}
