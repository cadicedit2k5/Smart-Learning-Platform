package com.smartlearning.ai.conversation.service;

import com.smartlearning.ai.conversation.dto.response.ChatMessageResponse;
import com.smartlearning.ai.conversation.dto.response.ChatTurnResponse;
import com.smartlearning.ai.conversation.dto.response.ConversationSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface ConversationService {

    ChatTurnResponse startConversation(UUID courseId, UUID userId, String accessToken, String content);

    ChatTurnResponse sendMessage(
            UUID courseId,
            UUID conversationId,
            UUID userId,
            String accessToken,
            String content
    );

    List<ConversationSummaryResponse> getConversations(UUID courseId, UUID userId, String accessToken);

    List<ChatMessageResponse> getMessages(
            UUID courseId,
            UUID conversationId,
            UUID userId,
            String accessToken
    );
}
