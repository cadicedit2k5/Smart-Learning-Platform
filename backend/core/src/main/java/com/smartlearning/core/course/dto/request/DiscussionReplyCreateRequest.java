package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record DiscussionReplyCreateRequest(
        @NotNull
        Map<String, Object> content
) {
}
