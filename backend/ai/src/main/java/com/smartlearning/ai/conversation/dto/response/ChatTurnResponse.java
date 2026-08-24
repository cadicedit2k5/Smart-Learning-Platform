package com.smartlearning.ai.conversation.dto.response;

import java.util.UUID;

public record ChatTurnResponse(
        UUID conversationId,
        ChatMessageResponse userMessage,
        ChatMessageResponse assistantMessage
) {
}
