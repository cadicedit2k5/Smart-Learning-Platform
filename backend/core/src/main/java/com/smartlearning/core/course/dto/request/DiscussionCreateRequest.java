package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record DiscussionCreateRequest(
        UUID topicId,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotNull
        Map<String, Object> content
) {
}