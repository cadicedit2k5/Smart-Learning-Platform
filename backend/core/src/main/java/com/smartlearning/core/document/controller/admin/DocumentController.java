package com.smartlearning.core.document.controller.admin;
import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.core.document.dto.request.AdminDocumentFilterRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.core.document.service.DocumentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/documents")
@RequiredArgsConstructor
@PreAuthorize(Authorities.COURSE_MANAGE)
public class DocumentController {

    private final DocumentAdminService documentAdminService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagingResponse<DocumentResponse>>> getDocuments(
            @ModelAttribute AdminDocumentFilterRequest filter) {
        return ApiResponses.ok(documentAdminService.handleGetAllForAdmin(filter));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @PathVariable UUID documentId) {
        return ApiResponses.ok(documentAdminService.handleGetDocumentForAdmin(documentId));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID documentId) {
        documentAdminService.handleDeleteDocumentForAdmin(documentId);

        return ApiResponses.noContent();
    }
}
