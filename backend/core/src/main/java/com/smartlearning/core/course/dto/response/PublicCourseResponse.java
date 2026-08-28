package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.CourseMemberStatus;

import java.time.Instant;
import java.util.UUID;

public record PublicCourseResponse(
        UUID id,
        String title,
        String description,
        String level,
        UUID createdBy,
        Instant publishedAt,
        CourseMemberStatus currentUserMembershipStatus
) {
}
