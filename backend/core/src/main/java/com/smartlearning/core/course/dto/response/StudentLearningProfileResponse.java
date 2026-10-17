package com.smartlearning.core.course.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StudentLearningProfileResponse(
        UUID courseId,
        UUID studentId,
        Instant joinedAt,
        Progress progress,
        Engagement engagement,
        Formative formative,
        Formal formal
) {

    public record Progress(
            long totalTopics,
            long completedTopics,
            long inProgressTopics,
            long notStartedTopics,
            int completionRatePercentage
    ) {
    }

    public record Engagement(
            long totalActiveSeconds,
            Instant lastLearningActivity
    ) {
    }

    public record Formative(
            long attemptCount,
            Double firstScorePercentage,
            Double latestScorePercentage,
            Double bestScorePercentage
    ) {
    }

    public record Formal(
            int submissionRatePercentage,
            Double averageGradedScorePercentage
    ) {
    }
}