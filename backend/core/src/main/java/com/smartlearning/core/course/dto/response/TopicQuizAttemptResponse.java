package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.QuestionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TopicQuizAttemptResponse(
        UUID id,
        UUID assessmentId,
        int version,
        int score,
        int maxScore,
        Instant submittedAt,
        List<AnswerReview> answers
) {
    public record AnswerReview(
            UUID questionId,
            QuestionType type,
            String prompt,
            List<Option> options,
            List<UUID> selectedOptionIds,
            List<UUID> correctOptionIds,
            int awardedScore,
            String explanation
    ) {}

    public record Option(
            UUID id,
            String content
    ) {}
}