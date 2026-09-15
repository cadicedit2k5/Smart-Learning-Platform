package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.CourseMemberStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicCourseDetailResponse(
        UUID id,
        String title,
        String description,
        String imageUrl,
        String level,
        Instant publishedAt,
        CourseMemberStatus currentUserMembershipStatus,
        List<ChapterOutline> chapters
) {
    public record ChapterOutline(
            UUID id,
            String title,
            String description,
            String learningObjectives,
            Integer orderIndex,
            List<TopicOutline> topics
    ) {
    }

    public record TopicOutline(
            UUID id,
            String title,
            String description,
            Integer orderIndex,
            Integer estimatedMinutes
    ) {
    }
}