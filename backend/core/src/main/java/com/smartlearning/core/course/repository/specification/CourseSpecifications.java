package com.smartlearning.core.course.repository.specification;

import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

@NoArgsConstructor
public class CourseSpecifications {

    public static Specification<Course> keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Specification.unrestricted();
        }

        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern));
    }

    public static Specification<Course> status(CourseStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Course> visibility(CourseVisibility visibility) {
        if (visibility == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("visibility"), visibility);
    }

    public static Specification<Course> notDeleted() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(root.get("deletedAt"));
    }

    public static Specification<Course> members(UUID userId) {
        return (root, query, criteriaBuilder) -> {
            Join<Course, CourseMember> member = root.join("members", JoinType.INNER);

            return criteriaBuilder.and(
                    criteriaBuilder.equal(member.get("userId"), userId),
                    criteriaBuilder.equal(member.get("status"), CourseMemberStatus.ACTIVE));
        };
    }
}
