package com.smartlearning.core.document.controller.api;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.document.dto.request.DocumentCreateRequest;
import com.smartlearning.core.document.dto.request.DocumentFilterRequest;
import com.smartlearning.core.document.dto.request.DocumentUpdateRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.core.document.service.DocumentService;
import com.smartlearning.storage.dto.StoredFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/courses/{courseId}/documents")
@RequiredArgsConstructor
public class ApiDocumentController {

    private final DocumentService documentService;

    @GetMapping
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<PagingResponse<DocumentResponse>>> get(
            @PathVariable UUID courseId,
            @ModelAttribute DocumentFilterRequest filter,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(documentService.handleGetAll(
                courseId,
                JwtUtils.getUserId(jwt),
                filter
        ));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<DocumentResponse>> create(
            @PathVariable UUID courseId,
            @Valid @ModelAttribute DocumentCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponses.created(documentService.handleCreateDocument(
                courseId,
                JwtUtils.getUserId(jwt),
                request
        ));
    }

    @GetMapping("/{documentId}")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @PathVariable UUID courseId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(documentService.handleGetDocument(
                courseId,
                documentId,
                JwtUtils.getUserId(jwt)
        ));
    }

    @PatchMapping("/{documentId}")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<DocumentResponse>> update(
            @PathVariable UUID courseId,
            @PathVariable UUID documentId,
            @Valid @RequestBody DocumentUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(documentService.handleUpdateDocument(
                courseId,
                documentId,
                JwtUtils.getUserId(jwt),
                request
        ));
    }

    @GetMapping("/{documentId}/download")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID courseId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        StoredFile file = documentService.handleDownloadDocument(
                courseId,
                documentId,
                JwtUtils.getUserId(jwt)
        );
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(file.contentType());
        } catch (IllegalArgumentException exception) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(file.size())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(new InputStreamResource(file.inputStream()));
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<Void> delete(
            @PathVariable UUID courseId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        documentService.handleDeleteDocument(
                courseId,
                documentId,
                JwtUtils.getUserId(jwt)
        );
        return ApiResponses.noContent();
    }
}
