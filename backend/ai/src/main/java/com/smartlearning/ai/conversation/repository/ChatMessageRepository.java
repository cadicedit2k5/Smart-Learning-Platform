package com.smartlearning.ai.conversation.repository;

import com.smartlearning.ai.conversation.entity.ChatMessage;
import com.smartlearning.ai.conversation.entity.enums.ChatAccessScope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<ChatMessage> findAllByConversationIdAndAccessScopeOrderByCreatedAtAsc(UUID conversationId, ChatAccessScope accessScope);

    List<ChatMessage>
    findAllByConversationIdOrderByCreatedAtDesc(
            UUID conversationId,
            Pageable pageable
    );
    List<ChatMessage>
    findAllByConversation_IdAndAccessScopeOrderByCreatedAtDesc(
            UUID conversationId,
            ChatAccessScope accessScope,
            Pageable pageable
    );
}