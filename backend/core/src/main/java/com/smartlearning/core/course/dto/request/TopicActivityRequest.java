package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.Positive;

public record TopicActivityRequest(
        @Positive long activeSeconds
) {
}