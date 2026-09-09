package com.smartlearning.core.course.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DiscussionResponse(
        UUID id,
        UUID courseId,
        UUID topicId,
        String topicTitle,
        UUID authorId,
        DiscussionAuthorResponse author,
        String title,
        Map<String, Object> content,
        List<DiscussionReplyResponse> replies,
        Instant createdAt,
        Instant updatedAt
) {
}
