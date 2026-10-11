package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.QuestionType;

import java.util.List;
import java.util.UUID;

public record TopicQuizStudentResponse(
        UUID id,
        UUID topicId,
        String title,
        String description,
        int version,
        List<Question> questions
) {
    public record Question(
            UUID id,
            QuestionType type,
            String prompt,
            List<Option> options
    ) {}

    public record Option(
            UUID id,
            String content
    ) {}
}