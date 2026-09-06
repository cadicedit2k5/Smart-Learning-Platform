package com.smartlearning.core.course.messaging.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TopicKnowledgeIndexRequestedEvent(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        UUID courseId,
        UUID chapterId,
        UUID topicId,
        String title,
        String description,
        Map<String, Object> content,
        TopicKnowledgeOperation operation
) {}