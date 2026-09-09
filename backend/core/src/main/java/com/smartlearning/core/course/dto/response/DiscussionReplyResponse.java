package com.smartlearning.core.course.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DiscussionReplyResponse(
        UUID id,
        UUID authorId,
        DiscussionAuthorResponse author,
        Map<String, Object> content,
        Instant createdAt
) {
}
