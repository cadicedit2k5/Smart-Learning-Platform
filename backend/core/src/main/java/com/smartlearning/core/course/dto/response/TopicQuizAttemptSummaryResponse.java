package com.smartlearning.core.course.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TopicQuizAttemptSummaryResponse(
        UUID id,
        UUID assessmentId,
        int version,
        int score,
        int maxScore,
        Instant submittedAt
) {}