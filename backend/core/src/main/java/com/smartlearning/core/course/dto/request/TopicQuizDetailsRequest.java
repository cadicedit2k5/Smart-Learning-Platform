package com.smartlearning.core.course.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record TopicQuizDetailsRequest(
        @Pattern(regexp = "(?s).*\\S.*")
        @Size(max = 255)
        String title,

        @Size(max = 10_000)
        String description,

        List<@NotNull @Valid ExplanationUpdate> explanations
) {
    public record ExplanationUpdate(
            @NotNull UUID questionId,
            String explanation
    ) {}
}
