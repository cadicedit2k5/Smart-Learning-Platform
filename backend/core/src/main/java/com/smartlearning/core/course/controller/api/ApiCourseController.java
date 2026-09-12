package com.smartlearning.core.course.controller.api;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.course.dto.request.*;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.dto.response.PublicCourseResponse;
import com.smartlearning.core.course.service.CourseService;
import com.smartlearning.storage.dto.StoredFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class ApiCourseController {

    private final CourseService courseService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @Valid @RequestPart("course") CourseCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.created(
                courseService.createCourse(request.withImage(image), JwtUtils.getUserId(jwt))
        );
    }

    @GetMapping("/public")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<PagingResponse<PublicCourseResponse>>> publicCourses(
            @ModelAttribute PublicCourseFilterRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(courseService.getPublicCourses(request, JwtUtils.getUserId(jwt)));
    }

    @GetMapping("/me")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<PagingResponse<CourseResponse>>> myCourses(
            @ModelAttribute MyCourseFilterRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(courseService.getMyCourses(JwtUtils.getUserId(jwt), request));
    }

    @GetMapping("/{courseId}")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<CourseResponse>> get(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(courseService.getCourse(courseId, JwtUtils.getUserId(jwt)));
    }

    @PatchMapping(value = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<CourseResponse>> update(
            @PathVariable UUID courseId,
            @Valid @RequestPart("course") CourseUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                courseService.updateCourse(courseId, request.withImage(image), JwtUtils.getUserId(jwt))
        );
    }

    @GetMapping("/{courseId}/image")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<InputStreamResource> image(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        StoredFile image = courseService.getCourseImage(courseId, JwtUtils.getUserId(jwt));

        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(image.contentType());
        } catch (IllegalArgumentException exception) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(image.size())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(image.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(new InputStreamResource(image.inputStream()));
    }

    @PostMapping("/{courseId}/publish")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<CourseResponse>> publish(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(courseService.publishCourse(courseId, JwtUtils.getUserId(jwt)));
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<Void> delete(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        courseService.deleteCourse(courseId, JwtUtils.getUserId(jwt));
        return ApiResponses.noContent();
    }
}