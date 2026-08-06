package com.smartlearning.core.document.controller.api;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.document.dto.request.DocumentCreateRequest;
import com.smartlearning.core.document.dto.response.DocumentResponse;
import com.smartlearning.core.document.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("courses/{courseId}/documents")
@RequiredArgsConstructor
public class ApiDocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<DocumentResponse>> create(
            @PathVariable UUID courseId,
            @Valid @ModelAttribute DocumentCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponses.created(documentService.createDocument(courseId, JwtUtils.getUserId(jwt), request));
    }
}
