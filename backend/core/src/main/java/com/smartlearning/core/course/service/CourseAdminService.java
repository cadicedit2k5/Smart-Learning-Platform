package com.smartlearning.core.course.service;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.course.dto.request.CourseFilterRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;

import java.util.UUID;

public interface CourseAdminService {

    PagingResponse<CourseResponse> getCoursesForAdmin(CourseFilterRequest filter);

    CourseResponse getCourseForAdmin(UUID courseId);

    CourseResponse updateCourseForAdmin(UUID courseId, CourseUpdateRequest request);

    void deleteCourseForAdmin(UUID courseId);
}
