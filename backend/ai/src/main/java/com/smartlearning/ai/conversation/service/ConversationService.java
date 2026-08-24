package com.smartlearning.ai.conversation.service;

import com.smartlearning.ai.conversation.dto.response.ChatTurnResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationService {

    ChatTurnResponse startConversation(UUID courseId, UUID userId, String accessToken, String content);
}
