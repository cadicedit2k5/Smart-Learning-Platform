package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseVisibility;

import java.time.Instant;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String title,
        String description,
        String level,
        CourseVisibility visibility,
        CourseStatus status,
        UUID createdBy,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        CourseMemberRole currentUserRole
) {
}
