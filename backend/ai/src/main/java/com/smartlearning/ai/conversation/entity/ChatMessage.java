package com.smartlearning.ai.conversation.entity;

import com.smartlearning.ai.conversation.entity.enums.ChatAccessScope;
import com.smartlearning.ai.conversation.entity.enums.ChatMessageRole;
import com.smartlearning.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "chat_message",
    schema = "ai_app",
    indexes = {
        @Index(
            name = "idx_chat_message_conversation",
            columnList = "conversation_id, created_at"
        )
    }
)
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AiConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "access_scope",
            nullable = false,
            length = 20
    )
    private ChatAccessScope accessScope;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}