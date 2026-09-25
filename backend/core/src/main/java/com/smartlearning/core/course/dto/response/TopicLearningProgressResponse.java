package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.LearningProgressStatus;

import java.time.Instant;
import java.util.UUID;

public record TopicLearningProgressResponse(
        UUID topicId,
        LearningProgressStatus status,
        long activeSeconds,
        long minimumCompletionSeconds,
        int studyPercentage,
        boolean canComplete,
        Instant startedAt,
        Instant completedAt,
        Instant lastAccessedAt
) {
}