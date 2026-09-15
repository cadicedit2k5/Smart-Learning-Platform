package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.CourseMemberStatus;

import java.time.Instant;
import java.util.UUID;

public record PublicCourseResponse(
        UUID id,
        String title,
        String description,
        String imageUrl,
        String level,
        Instant publishedAt,
        CourseMemberStatus currentUserMembershipStatus
) {
    public PublicCourseResponse(UUID id, String title, String description, String level, Instant publishedAt,
                                CourseMemberStatus currentUserMembershipStatus) {
        this(id, title, description, null, level, publishedAt, currentUserMembershipStatus);
    }
}