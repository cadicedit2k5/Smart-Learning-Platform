package com.smartlearning.core.course.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record TopicQuizSubmitRequest(
        @NotNull List<@NotNull @Valid Answer> answers
) {
    public record Answer(
            @NotNull UUID questionId,
            @NotNull List<@NotNull UUID> selectedOptionIds
    ) {}
}