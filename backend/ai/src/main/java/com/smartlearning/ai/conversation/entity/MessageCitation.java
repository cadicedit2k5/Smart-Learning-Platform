package com.smartlearning.ai.conversation.entity;


import com.smartlearning.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
    name = "message_citation",
    schema = "ai_app",
    indexes = {
        @Index(
                name = "idx_message_citation_message",
                columnList = "message_id"
        )
    }
)
public class MessageCitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "document_version_id")
    private UUID documentVersionId;

    @Column(name = "locator_json", columnDefinition = "TEXT")
    private String locatorJson;
}