package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.QuestionType;
import com.smartlearning.core.course.entity.enums.TopicQuizStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TopicQuizEditorResponse(
        UUID id,
        UUID topicId,
        String title,
        String description,
        int version,
        TopicQuizStatus status,
        boolean hasAttempts,
        Instant publishedAt,
        List<Question> questions
) {
    public record Question(
            UUID id,
            QuestionType type,
            String prompt,
            String explanation,
            List<Option> options
    ) {}

    public record Option(
            UUID id,
            String content,
            boolean correct
    ) {}
}