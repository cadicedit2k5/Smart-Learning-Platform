package com.smartlearning.core.document.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.document.dto.request.AdminDocumentFilterRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.mapper.DocumentMapper;
import com.smartlearning.core.document.repository.DocumentRepository;
import com.smartlearning.core.document.service.DocumentAdminService;
import com.smartlearning.core.document.service.DocumentDeletionService;
import com.smartlearning.core.document.utils.DocumentUtils;
import com.smartlearning.storage.dto.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentAdminServiceImpl implements DocumentAdminService {
    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final DocumentUtils documentUtils;
    private final DocumentDeletionService documentDeletionService;

    @Override
    @Transactional(readOnly = true)
    public PagingResponse<DocumentResponse> handleGetAllForAdmin(AdminDocumentFilterRequest filter) {
        Page<DocumentResponse> documents = documentRepository.findAll(
                                filter.specification(),
                                filter.pageable()).map(
                                documentMapper::toResponse);

        return PagingResponse.from(documents);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse handleGetDocumentForAdmin(UUID documentId) {
        Document document = documentUtils.requireDocument(documentId);

        return documentMapper.toResponse(document);
    }

    @Override
    public StoredFile handleDownloadDocumentForAdmin(UUID documentId) {
        Document document = documentUtils.requireDocument(documentId);

        return documentUtils.downloadDocument(document);
    }

    @Override
    @Transactional
    public void handleDeleteDocumentForAdmin(UUID documentId) {
        Document document = documentUtils.requireDocument(documentId);

        documentDeletionService.deleteDocument(
                document.getCourse().getId(),
                documentId
        );
    }
}
