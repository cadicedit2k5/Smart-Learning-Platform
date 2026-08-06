package com.smartlearning.core.document.service;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.document.dto.request.DocumentCreateRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;

import java.util.UUID;

public interface DocumentService {
    DocumentResponse createDocument(UUID courseId, UUID currentUserId,
            DocumentCreateRequest request);


}
