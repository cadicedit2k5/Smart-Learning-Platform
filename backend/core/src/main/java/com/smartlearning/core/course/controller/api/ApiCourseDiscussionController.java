package com.smartlearning.core.course.controller.api;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.course.dto.request.DiscussionCreateRequest;
import com.smartlearning.core.course.dto.request.DiscussionReplyCreateRequest;
import com.smartlearning.core.course.dto.response.DiscussionResponse;
import com.smartlearning.core.course.service.CourseDiscussionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses/{courseId}/discussions")
@RequiredArgsConstructor
public class ApiCourseDiscussionController {

    private final CourseDiscussionService discussionService;

    @GetMapping
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<List<DiscussionResponse>>> getDiscussions(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                discussionService.getDiscussions(
                        courseId,
                        JwtUtils.getUserId(jwt),
                        jwt.getTokenValue()
                )
        );
    }

    @PostMapping
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<DiscussionResponse>> createDiscussion(
            @PathVariable UUID courseId,
            @Valid @RequestBody DiscussionCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.created(
                discussionService.createDiscussion(
                        courseId,
                        JwtUtils.getUserId(jwt),
                        jwt.getTokenValue(),
                        request
                )
        );
    }

    @PostMapping("/{discussionId}/replies")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<DiscussionResponse>> createReply(
            @PathVariable UUID courseId,
            @PathVariable UUID discussionId,
            @Valid @RequestBody DiscussionReplyCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.created(
                discussionService.createReply(
                        courseId,
                        discussionId,
                        JwtUtils.getUserId(jwt),
                        jwt.getTokenValue(),
                        request
                )
        );
    }
}
