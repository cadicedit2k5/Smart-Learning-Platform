package com.smartlearning.core.course.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentLearningProgressDetailResponse(
        UUID studentId,
        String fullName,
        String email,
        long totalTopics,
        long completedTopics,
        long inProgressTopics,
        int progressPercentage,
        List<ChapterProgress> chapters
) {
    public record ChapterProgress(
            UUID chapterId,
            String title,
            Integer orderIndex,
            long totalTopics,
            long completedTopics,
            int progressPercentage,
            List<TopicProgress> topics
    ) {
    }

    public record TopicProgress(
            UUID topicId,
            String title,
            Integer orderIndex,
            String status,
            Instant startedAt,
            Instant completedAt,
            Instant lastAccessedAt
    ) {
    }
}
