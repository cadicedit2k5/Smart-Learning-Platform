package com.smartlearning.core.course.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record TopicQuizCreateRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 10_000) String description,
        @NotEmpty List<@NotNull @Valid TopicQuizQuestionRequest> questions
) {}