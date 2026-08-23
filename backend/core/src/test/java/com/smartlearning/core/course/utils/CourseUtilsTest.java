package com.smartlearning.core.course.utils;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.course;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseUtilsTest {

    @Mock
    private CourseRepository courseRepository;
    @InjectMocks
    private CourseUtils courseUtils;

    @Test
    void requireCourse_returnsNonDeletedCourse() {
        Course expected = course();
        when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(expected));

        assertThat(courseUtils.requireCourse(COURSE_ID)).isSameAs(expected);
    }

    @Test
    void requireCourse_mapsMissingOrDeletedCourseToNotFound() {
        when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseUtils.requireCourse(COURSE_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
    }
}
