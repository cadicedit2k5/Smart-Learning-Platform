package com.smartlearning.core.course.controller.api;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.course.dto.response.CourseLearningProgressResponse;
import com.smartlearning.core.course.dto.response.LecturerCourseProgressResponse;
import com.smartlearning.core.course.dto.response.StudentLearningProgressDetailResponse;
import com.smartlearning.core.course.dto.response.TopicLearningProgressResponse;
import com.smartlearning.core.course.service.LearningProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/courses/{courseId}/learning")
@RequiredArgsConstructor
public class ApiLearningProgressController {

    private final LearningProgressService progressService;

    @PostMapping("/topics/{topicId}/start")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<TopicLearningProgressResponse>> startTopic(
            @PathVariable UUID courseId,
            @PathVariable UUID topicId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                progressService.startTopic(courseId, topicId, JwtUtils.getUserId(jwt))
        );
    }

    @PostMapping("/topics/{topicId}/complete")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<TopicLearningProgressResponse>> completeTopic(
            @PathVariable UUID courseId,
            @PathVariable UUID topicId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                progressService.completeTopic(courseId, topicId, JwtUtils.getUserId(jwt))
        );
    }

    @GetMapping("/progress/me")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<CourseLearningProgressResponse>> getMyProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                progressService.getMyCourseProgress(courseId, JwtUtils.getUserId(jwt))
        );
    }

    @GetMapping("/progress/students")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<LecturerCourseProgressResponse>> getCourseStudentProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                progressService.getCourseStudentProgress(
                        courseId,
                        JwtUtils.getUserId(jwt),
                        jwt.getTokenValue()
                )
        );
    }

    @GetMapping("/progress/students/{studentId}")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<StudentLearningProgressDetailResponse>> getStudentProgress(
            @PathVariable UUID courseId,
            @PathVariable UUID studentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                progressService.getStudentProgress(
                        courseId,
                        studentId,
                        JwtUtils.getUserId(jwt),
                        jwt.getTokenValue()
                )
        );
    }
}