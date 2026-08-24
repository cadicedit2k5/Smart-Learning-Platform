package com.smartlearning.ai.conversation.repository;

import com.smartlearning.ai.conversation.entity.MessageCitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageCitationRepository extends JpaRepository<MessageCitation, UUID> {

    List<MessageCitation> findAllByMessageId(UUID messageId);
}