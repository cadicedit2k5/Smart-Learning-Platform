package com.smartlearning.ai.conversation.repository;

import com.smartlearning.ai.conversation.entity.AiConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {

    Optional<AiConversation> findByIdAndCourseIdAndUserIdAndDeletedAtIsNull(
            UUID id,
            UUID courseId,
            UUID userId
    );

    Page<AiConversation> findAllByCourseIdAndUserIdAndDeletedAtIsNullOrderByLastMessageAtDesc(
            UUID courseId,
            UUID userId,
            Pageable pageable
    );
}
