package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.course.dto.request.CourseFilterRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.mapper.CourseMapper;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.service.CourseAdminService;
import com.smartlearning.core.course.utils.CourseUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseAdminServiceImpl implements CourseAdminService {
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final CourseUtils courseUtils;

    @Override
    public PagingResponse<CourseResponse> getCoursesForAdmin(CourseFilterRequest filter) {
        Page<CourseResponse> pages = courseRepository.findAll(filter.specification(), filter.pageable())
                        .map(course -> courseUtils.withRole(courseMapper.toResponse(course), null));

        return PagingResponse.from(pages);
    }

    @Override
    public CourseResponse getCourseForAdmin(UUID courseId) {
        Course course = courseUtils.requireCourse(courseId);

        return courseUtils.withRole(courseMapper.toResponse(course), null);
    }

    @Override
    public CourseResponse updateCourseForAdmin(UUID courseId, CourseUpdateRequest request) {
        Course course = courseUtils.requireCourse(courseId);

        courseMapper.partialUpdate(request, course);

        if (course.getTitle() != null) {
            course.setTitle(course.getTitle().trim());
        }
        return courseUtils.withRole(courseMapper.toResponse(course), null);
    }

    @Override
    public void deleteCourseForAdmin(UUID courseId) {
        Course course = courseUtils.requireCourse(courseId);

        course.setDeletedAt(Instant.now());
    }
}
