package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;

import java.time.Instant;
import java.util.UUID;

public record CourseMemberDetailResponse(
        UUID id,
        UUID courseId,
        UUID userId,
        CourseMemberUserResponse user,
        CourseMemberRole role,
        CourseMemberStatus status,
        Instant joinedAt,
        UUID invitedBy,
        Instant removedAt,
        Instant createdAt
) {
}
