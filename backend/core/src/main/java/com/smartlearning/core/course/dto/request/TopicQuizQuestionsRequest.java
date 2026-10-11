package com.smartlearning.core.course.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record TopicQuizQuestionsRequest(
        @NotEmpty List<@NotNull @Valid TopicQuizQuestionRequest> questions
) {}
