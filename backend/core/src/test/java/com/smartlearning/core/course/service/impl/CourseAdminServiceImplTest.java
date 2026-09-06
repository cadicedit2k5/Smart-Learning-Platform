package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.course.dto.request.CourseFilterRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.mapper.CourseMapper;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.utils.CourseUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.course;
import static com.smartlearning.core.support.CoreTestData.courseResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAdminServiceImplTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private CourseUtils courseUtils;
    @InjectMocks
    private CourseAdminServiceImpl adminService;

    @Test
    void getCoursesForAdmin_mapsRepositoryPageWithoutMemberRole() {
        CourseFilterRequest filter = new CourseFilterRequest();
        Course course = course();
        CourseResponse mapped = courseResponse(course);
        when(courseRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Course>>any(),
                eq(filter.pageable())
        ))
                .thenReturn(new PageImpl<>(List.of(course), filter.pageable(), 1));
        when(courseMapper.toResponse(course)).thenReturn(mapped);
        when(courseUtils.withRole(mapped, null)).thenReturn(mapped);

        PagingResponse<CourseResponse> result = adminService.getCoursesForAdmin(filter);

        assertThat(result.getContent()).containsExactly(mapped);
        assertThat(result.getPageable().getTotalElements()).isOne();
    }

    @Test
    void getCourseForAdmin_returnsCourseWithoutRequiringMembership() {
        Course course = course();
        CourseResponse expected = courseResponse(course);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course);
        when(courseMapper.toResponse(course)).thenReturn(expected);
        when(courseUtils.withRole(expected, null)).thenReturn(expected);

        assertThat(adminService.getCourseForAdmin(COURSE_ID)).isSameAs(expected);
    }

    @Test
    void updateCourseForAdmin_appliesPartialUpdateAndTrimsTitle() {
        Course course = course();
        CourseUpdateRequest request = new CourseUpdateRequest(
                "  Updated title  ",
                null,
                null,
                CourseVisibility.PUBLIC
        );
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course);
        org.mockito.Mockito.doAnswer(invocation -> {
            course.setTitle(request.title());
            course.setVisibility(request.visibility());
            return null;
        }).when(courseMapper).partialUpdate(request, course);
        when(courseMapper.toResponse(course)).thenAnswer(invocation -> courseResponse(course));
        when(courseUtils.withRole(any(CourseResponse.class), org.mockito.ArgumentMatchers.isNull()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseResponse result = adminService.updateCourseForAdmin(COURSE_ID, request);

        assertThat(result.title()).isEqualTo("Updated title");
        assertThat(result.visibility()).isEqualTo(CourseVisibility.PUBLIC);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void deleteCourseForAdmin_softDeletesCourse() {
        Course course = course();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course);

        Instant beforeCall = Instant.now();
        adminService.deleteCourseForAdmin(COURSE_ID);
        Instant afterCall = Instant.now();

        assertThat(course.getDeletedAt()).isBetween(beforeCall, afterCall);
        verify(courseRepository, never()).delete(any(Course.class));
    }
}
