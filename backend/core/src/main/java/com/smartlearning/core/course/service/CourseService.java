package com.smartlearning.core.course.service;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.course.dto.request.*;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.dto.response.PublicCourseDetailResponse;
import com.smartlearning.core.course.dto.response.PublicCourseResponse;
import com.smartlearning.storage.dto.StoredFile;

import java.util.UUID;

public interface CourseService {

    CourseResponse createCourse(CourseCreateRequest request, UUID currentUserId);

    CourseResponse updateFeatureConfig(UUID courseId, CourseFeatureConfigRequest request, UUID currentUserId);

    CourseResponse getCourse(UUID courseId, UUID currentUserId);

    PagingResponse<CourseResponse> getMyCourses(UUID currentUserId, MyCourseFilterRequest request);

    PagingResponse<PublicCourseResponse> getPublicCourses(PublicCourseFilterRequest request, UUID currentUserId);

    PublicCourseDetailResponse getPublicCourseDetail(UUID courseId, UUID currentUserId);

    CourseResponse updateCourse(UUID courseId, CourseUpdateRequest request, UUID currentUserId);

    StoredFile getCourseImage(UUID courseId, UUID currentUserId);

    CourseResponse publishCourse(UUID courseId, UUID currentUserId);

    void deleteCourse(UUID courseId, UUID currentUserId);
}