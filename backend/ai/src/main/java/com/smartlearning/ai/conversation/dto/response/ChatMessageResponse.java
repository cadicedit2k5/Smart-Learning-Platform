package com.smartlearning.ai.conversation.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        String role,
        String accessScope,
        String content,
        List<Citation> citations,
        Instant createdAt
) {

    public record Citation(
            String label,
            UUID chunkId,
            UUID documentId,
            UUID documentVersionId,
            Map<String, Object> locator
    ) {
    }
}
