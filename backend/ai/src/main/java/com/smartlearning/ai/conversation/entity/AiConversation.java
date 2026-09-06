package com.smartlearning.ai.conversation.entity;


import com.smartlearning.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
    name = "conversation",
    schema = "ai_app",
    indexes = {
        @Index(
            name = "idx_conversation_user_course",
            columnList = "user_id, course_id"
        )
    }
)
public class AiConversation extends BaseEntity {

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}