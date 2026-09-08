package com.smartlearning.core.course.dto.request;

import com.smartlearning.common.dto.request.FilterRequest;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.repository.specification.CourseSpecifications;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

@Getter
@Setter
public class PublicCourseFilterRequest extends FilterRequest<Course> {

    private String keyword;

    @Override
    public Specification<Course> specification() {
        return Specification.allOf(
                CourseSpecifications.keyword(keyword),
                CourseSpecifications.visibility(CourseVisibility.PUBLIC),
                CourseSpecifications.notDeleted()
        );
    }
}
