package com.smartlearning.core.document.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.document.dto.request.AdminDocumentFilterRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.core.document.entity.Document;
import com.smartlearning.core.document.mapper.DocumentMapper;
import com.smartlearning.core.document.repository.DocumentRepository;
import com.smartlearning.core.document.service.DocumentDeletionService;
import com.smartlearning.core.document.utils.DocumentUtils;
import com.smartlearning.storage.dto.StoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayInputStream;
import java.util.List;

import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.DOCUMENT_ID;
import static com.smartlearning.core.support.CoreTestData.document;
import static com.smartlearning.core.support.CoreTestData.documentResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAdminServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private DocumentUtils documentUtils;
    @Mock
    private DocumentDeletionService documentDeletionService;
    @InjectMocks
    private DocumentAdminServiceImpl adminService;

    @Test
    void getAllForAdmin_mapsFilteredRepositoryPage() {
        AdminDocumentFilterRequest filter = new AdminDocumentFilterRequest();
        Document document = document();
        DocumentResponse mapped = documentResponse();
        when(documentRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Document>>any(),
                eq(filter.pageable())
        ))
                .thenReturn(new PageImpl<>(List.of(document), filter.pageable(), 1));
        when(documentMapper.toResponse(document)).thenReturn(mapped);

        PagingResponse<DocumentResponse> result = adminService.handleGetAllForAdmin(filter);

        assertThat(result.getContent()).containsExactly(mapped);
        assertThat(result.getPageable().getTotalElements()).isOne();
    }

    @Test
    void getDocumentForAdmin_usesSharedDocumentLookup() {
        Document document = document();
        DocumentResponse expected = documentResponse();
        when(documentUtils.requireDocument(DOCUMENT_ID)).thenReturn(document);
        when(documentMapper.toResponse(document)).thenReturn(expected);

        assertThat(adminService.handleGetDocumentForAdmin(DOCUMENT_ID)).isSameAs(expected);
    }

    @Test
    void downloadDocumentForAdmin_usesCurrentVersion() {
        Document document = document();
        StoredFile expected = new StoredFile(
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                "application/pdf",
                3,
                "lesson.pdf"
        );
        when(documentUtils.requireDocument(DOCUMENT_ID)).thenReturn(document);
        when(documentUtils.downloadDocument(document)).thenReturn(expected);

        assertThat(adminService.handleDownloadDocumentForAdmin(DOCUMENT_ID)).isSameAs(expected);
    }

    @Test
    void deleteDocumentForAdmin_resolvesCourseAndDelegatesDeletion() {
        Document document = document();
        when(documentUtils.requireDocument(DOCUMENT_ID)).thenReturn(document);

        adminService.handleDeleteDocumentForAdmin(DOCUMENT_ID);

        verify(documentDeletionService).deleteDocument(COURSE_ID, DOCUMENT_ID);
    }
}
