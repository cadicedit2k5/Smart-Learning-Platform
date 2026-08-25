package com.smartlearning.ai.conversation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryResponse(
        UUID id,
        String title,
        Instant lastMessageAt,
        Instant createdAt
) {
}
