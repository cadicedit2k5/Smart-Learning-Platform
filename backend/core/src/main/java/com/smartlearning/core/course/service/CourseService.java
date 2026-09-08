package com.smartlearning.core.course.service;

import com.smartlearning.core.course.dto.request.CourseCreateRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.request.MyCourseFilterRequest;
import com.smartlearning.core.course.dto.request.PublicCourseFilterRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.dto.response.PublicCourseResponse;
import com.smartlearning.common.dto.request.PagingRequest;
import com.smartlearning.common.dto.response.pagination.PagingResponse;

import java.util.List;
import java.util.UUID;

public interface CourseService {
    CourseResponse createCourse(
            CourseCreateRequest request,
            UUID currentUserId
    );

    CourseResponse getCourse(
            UUID courseId,
            UUID currentUserId
    );

    PagingResponse<CourseResponse> getMyCourses(
            UUID currentUserId,
            MyCourseFilterRequest request
    );

    PagingResponse<PublicCourseResponse> getPublicCourses(
            PublicCourseFilterRequest request,
            UUID currentUserId
    );

    CourseResponse updateCourse(
            UUID courseId,
            CourseUpdateRequest request,
            UUID currentUserId
    );

    CourseResponse publishCourse(
            UUID courseId,
            UUID currentUserId
    );

    void deleteCourse(
            UUID courseId,
            UUID currentUserId
    );
}
