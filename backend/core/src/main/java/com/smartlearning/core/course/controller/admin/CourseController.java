package com.smartlearning.core.course.controller.admin;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.core.course.dto.request.CourseFilterRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.service.CourseAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/courses")
@RequiredArgsConstructor
@PreAuthorize(Authorities.COURSE_MANAGE)
public class CourseController {

    private final CourseAdminService courseAdminService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagingResponse<CourseResponse>>> getCourses(
            @ModelAttribute CourseFilterRequest filter) {
        return ApiResponses.ok(courseAdminService.getCoursesForAdmin(filter));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(
            @PathVariable UUID courseId) {
        return ApiResponses.ok(courseAdminService.getCourseForAdmin(courseId));
    }

    @PatchMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable UUID courseId,
            @Valid
            @RequestBody
            CourseUpdateRequest request) {
        return ApiResponses.ok(courseAdminService.updateCourseForAdmin(courseId, request));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID courseId) {
        courseAdminService.deleteCourseForAdmin(courseId);

        return ApiResponses.noContent();
    }
}