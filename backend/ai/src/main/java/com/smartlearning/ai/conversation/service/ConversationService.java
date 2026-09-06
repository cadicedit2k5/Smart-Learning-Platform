package com.smartlearning.ai.conversation.service;

import com.smartlearning.ai.conversation.dto.response.ChatMessageResponse;
import com.smartlearning.ai.conversation.dto.response.ChatTurnResponse;
import com.smartlearning.ai.conversation.dto.response.ConversationSummaryResponse;
import com.smartlearning.common.dto.request.PagingRequest;
import com.smartlearning.common.dto.response.pagination.PagingResponse;

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

    PagingResponse<ConversationSummaryResponse> getConversations(
            UUID courseId,
            UUID userId,
            String accessToken,
            PagingRequest pagingRequest
    );

    PagingResponse<ChatMessageResponse> getMessages(
            UUID courseId,
            UUID conversationId,
            UUID userId,
            String accessToken,
            PagingRequest pagingRequest
    );
}
