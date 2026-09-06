package com.smartlearning.core.course.controller.api;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.course.dto.request.CourseChapterCreateRequest;
import com.smartlearning.core.course.dto.request.CourseChapterUpdateRequest;
import com.smartlearning.core.course.dto.request.CourseTopicCreateRequest;
import com.smartlearning.core.course.dto.request.CourseTopicUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseChapterResponse;
import com.smartlearning.core.course.dto.response.CourseTopicResponse;
import com.smartlearning.core.course.service.CourseContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses/{courseId}/chapters")
@RequiredArgsConstructor
public class ApiCourseContentController {

    private final CourseContentService contentService;

    @GetMapping
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<List<CourseChapterResponse>>> getChapters(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(contentService.getChapters(courseId, JwtUtils.getUserId(jwt)));
    }

    @PostMapping
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<CourseChapterResponse>> createChapter(
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseChapterCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.created(contentService.createChapter(
                courseId,
                request,
                JwtUtils.getUserId(jwt)
        ));
    }

    @PatchMapping("/{chapterId}")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<CourseChapterResponse>> updateChapter(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @Valid @RequestBody CourseChapterUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(contentService.updateChapter(
                courseId,
                chapterId,
                request,
                JwtUtils.getUserId(jwt)
        ));
    }

    @DeleteMapping("/{chapterId}")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<Void> deleteChapter(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        contentService.deleteChapter(courseId, chapterId, JwtUtils.getUserId(jwt));
        return ApiResponses.noContent();
    }

    @GetMapping("/{chapterId}/topics")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<List<CourseTopicResponse>>> getTopics(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(contentService.getTopics(
                courseId,
                chapterId,
                JwtUtils.getUserId(jwt)
        ));
    }

    @PostMapping("/{chapterId}/topics")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<CourseTopicResponse>> createTopic(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @Valid @RequestBody CourseTopicCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.created(contentService.createTopic(
                courseId,
                chapterId,
                request,
                JwtUtils.getUserId(jwt)
        ));
    }

    @PatchMapping("/{chapterId}/topics/{topicId}")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<CourseTopicResponse>> updateTopic(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @PathVariable UUID topicId,
            @Valid @RequestBody CourseTopicUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(contentService.updateTopic(
                courseId,
                chapterId,
                topicId,
                request,
                JwtUtils.getUserId(jwt)
        ));
    }

    @DeleteMapping("/{chapterId}/topics/{topicId}")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<Void> deleteTopic(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @PathVariable UUID topicId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        contentService.deleteTopic(courseId, chapterId, topicId, JwtUtils.getUserId(jwt));
        return ApiResponses.noContent();
    }
}
