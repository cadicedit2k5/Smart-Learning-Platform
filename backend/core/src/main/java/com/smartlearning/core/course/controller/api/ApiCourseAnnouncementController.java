package com.smartlearning.core.course.controller.api;

import com.smartlearning.common.dto.request.PagingRequest;
import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.course.dto.request.AnnouncementCreateRequest;
import com.smartlearning.core.course.dto.request.AnnouncementUpdateRequest;
import com.smartlearning.core.course.dto.response.AnnouncementResponse;
import com.smartlearning.core.course.service.CourseAnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/courses/{courseId}/announcements")
@RequiredArgsConstructor
public class ApiCourseAnnouncementController {

    private final CourseAnnouncementService announcementService;

    @GetMapping
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<PagingResponse<AnnouncementResponse>>> getAnnouncements(
            @PathVariable UUID courseId,
            @ModelAttribute PagingRequest pagingRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                announcementService.getAnnouncements(
                        courseId,
                        JwtUtils.getUserId(jwt),
                        pagingRequest
                )
        );
    }

    @PostMapping
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @PathVariable UUID courseId,
            @Valid @RequestBody AnnouncementCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.created(
                announcementService.createAnnouncement(
                        courseId,
                        JwtUtils.getUserId(jwt),
                        request
                )
        );
    }

    @PatchMapping("/{announcementId}")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable UUID courseId,
            @PathVariable UUID announcementId,
            @Valid @RequestBody AnnouncementUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                announcementService.updateAnnouncement(
                        courseId,
                        announcementId,
                        JwtUtils.getUserId(jwt),
                        request
                )
        );
    }

    @DeleteMapping("/{announcementId}")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<Void> deleteAnnouncement(
            @PathVariable UUID courseId,
            @PathVariable UUID announcementId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        announcementService.deleteAnnouncement(
                courseId,
                announcementId,
                JwtUtils.getUserId(jwt)
        );

        return ApiResponses.noContent();
    }
}