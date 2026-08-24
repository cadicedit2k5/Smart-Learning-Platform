package com.smartlearning.ai.conversation.repository;

import com.smartlearning.ai.conversation.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {

    Optional<AiConversation> findByIdAndCourseIdAndUserIdAndDeletedAtIsNull(
            UUID id,
            UUID courseId,
            UUID userId
    );

    List<AiConversation> findAllByCourseIdAndUserIdAndDeletedAtIsNullOrderByLastMessageAtDesc(
            UUID courseId,
            UUID userId
    );
}