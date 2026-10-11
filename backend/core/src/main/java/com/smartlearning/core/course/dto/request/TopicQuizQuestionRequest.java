package com.smartlearning.core.course.dto.request;

import com.smartlearning.core.course.entity.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record TopicQuizQuestionRequest(
        @NotNull QuestionType type,
        @NotBlank String content,
        String explanation,
        @NotNull @Size(min = 2) List<@NotNull @Valid OptionRequest> options
) {
    public record OptionRequest(
            @NotBlank String content,
            boolean correct
    ) {}
}