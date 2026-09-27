package com.smartlearning.core.course.activity;

import java.math.BigDecimal;
import java.util.UUID;

public sealed interface LearningActivityData {

    record TopicStartedData(UUID topicId) implements LearningActivityData {
    }

    record TopicCompletedData(UUID topicId, long activeSeconds) implements LearningActivityData {
    }

    record AssignmentSubmittedData(UUID assignmentId, UUID submissionId) implements LearningActivityData {
    }

    record AssignmentGradedData(
            UUID assignmentId,
            UUID submissionId,
            BigDecimal score,
            BigDecimal maxScore
    ) implements LearningActivityData {
    }
}