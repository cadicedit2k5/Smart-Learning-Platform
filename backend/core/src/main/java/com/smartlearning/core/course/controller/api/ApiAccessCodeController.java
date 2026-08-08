package com.smartlearning.core.course.controller.api;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.course.dto.request.AccessCodeCreateRequest;
import com.smartlearning.core.course.dto.response.AccessCodeCreatedResponse;
import com.smartlearning.core.course.service.AccessCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/courses/{courseId}/access-codes")
@RequiredArgsConstructor
public class ApiAccessCodeController {

    private final AccessCodeService accessCodeService;

    @PreAuthorize(Authorities.COURSE_MANAGE)
    @PostMapping
    public ResponseEntity<
                ApiResponse<AccessCodeCreatedResponse>
                > create(
            @PathVariable UUID courseId,
            @Valid @RequestBody
            AccessCodeCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.created(
                accessCodeService.createCode(
                        courseId,
                        request,
                        JwtUtils.getUserId(jwt)
                )
        );
    }

    @PreAuthorize(Authorities.COURSE_MANAGE)
    @DeleteMapping("/{codeId}")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID courseId,
            @PathVariable UUID codeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        accessCodeService.revokeCode(
                courseId,
                codeId,
                JwtUtils.getUserId(jwt)
        );

        return ApiResponses.noContent();
    }
}
