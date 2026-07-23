package com.smartlearning.core.course.controller.api;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.core.course.dto.request.CourseCreateRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class ApiCourseController {
    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @Valid @RequestBody CourseCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        return ApiResponses.created(
                courseService.createCourse(request, userId)
        );
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> get(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                courseService.getCourse(
                        courseId,
                        UUID.fromString(jwt.getSubject())
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> myCourses(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                courseService.getMyCourses(
                        UUID.fromString(jwt.getSubject())
                )
        );
    }

    @PatchMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> update(
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                courseService.updateCourse(
                        courseId,
                        request,
                        UUID.fromString(jwt.getSubject())
                )
        );
    }

    @PostMapping("/{courseId}/publish")
    public ResponseEntity<ApiResponse<CourseResponse>> publish(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                courseService.publishCourse(
                        courseId,
                        UUID.fromString(jwt.getSubject())
                )
        );
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        courseService.deleteCourse(
                courseId,
                UUID.fromString(jwt.getSubject())
        );

        return ApiResponses.noContent();
    }
 }
