package com.smartlearning.ai.infrastructure.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record RagRequest(

        @JsonProperty("course_id")
        UUID courseId,

        String question
) {
}