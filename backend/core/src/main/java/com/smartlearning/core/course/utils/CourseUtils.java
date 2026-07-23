package com.smartlearning.core.course.utils;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourseUtils {
    private final CourseRepository courseRepository;

    public Course requireCourse(UUID courseId) {
        return courseRepository
                .findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND
                ));
    }
}
