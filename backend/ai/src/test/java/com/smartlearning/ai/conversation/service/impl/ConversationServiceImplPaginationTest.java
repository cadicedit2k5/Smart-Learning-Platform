package com.smartlearning.ai.conversation.service.impl;

import com.smartlearning.ai.conversation.config.ConversationProperties;
import com.smartlearning.ai.conversation.dto.response.ChatMessageResponse;
import com.smartlearning.ai.conversation.dto.response.ConversationSummaryResponse;
import com.smartlearning.ai.conversation.entity.AiConversation;
import com.smartlearning.ai.conversation.entity.ChatMessage;
import com.smartlearning.ai.conversation.entity.MessageCitation;
import com.smartlearning.ai.conversation.entity.enums.ChatAccessScope;
import com.smartlearning.ai.conversation.entity.enums.ChatMessageRole;
import com.smartlearning.ai.conversation.repository.AiConversationRepository;
import com.smartlearning.ai.conversation.repository.ChatMessageRepository;
import com.smartlearning.ai.conversation.repository.MessageCitationRepository;
import com.smartlearning.ai.infrastructure.http.CoreCourseAccessClient;
import com.smartlearning.ai.infrastructure.http.PythonAiEngineClient;
import com.smartlearning.common.dto.request.PagingRequest;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplPaginationTest {

    @Mock
    private AiConversationRepository conversationRepository;
    @Mock
    private ChatMessageRepository messageRepository;
    @Mock
    private MessageCitationRepository citationRepository;
    @Mock
    private CoreCourseAccessClient courseAccessClient;
    @Mock
    private PythonAiEngineClient aiEngineClient;
    @Mock
    private TransactionTemplate transactionTemplate;

    private ConversationServiceImpl conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationServiceImpl(
                new ConversationProperties(20),
                conversationRepository,
                messageRepository,
                citationRepository,
                courseAccessClient,
                aiEngineClient,
                transactionTemplate
        );
    }

    @Test
    void paginatesConversations() {
        UUID courseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiConversation conversation = conversation(courseId, userId);
        Pageable pageable = PageRequest.of(0, 10);

        when(courseAccessClient.getAiAccess(courseId, "token")).thenReturn(fullAccess());
        when(conversationRepository.findAllByCourseIdAndUserIdAndDeletedAtIsNullOrderByLastMessageAtDesc(
                courseId,
                userId,
                pageable
        )).thenReturn(new PageImpl<>(List.of(conversation), pageable, 12));

        PagingResponse<ConversationSummaryResponse> response = conversationService.getConversations(
                courseId,
                userId,
                "token",
                new PagingRequest()
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).id()).isEqualTo(conversation.getId());
        assertThat(response.getPageable().getTotalElements()).isEqualTo(12);
        assertThat(response.getPageable().getTotalPages()).isEqualTo(2);
    }

    @Test
    void paginatesMessagesAndMapsCitations() {
        UUID courseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        AiConversation conversation = conversation(courseId, userId);
        conversation.setId(conversationId);

        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setConversation(conversation);
        message.setRole(ChatMessageRole.ASSISTANT);
        message.setAccessScope(ChatAccessScope.FULL);
        message.setContent("Answer");
        message.setCreatedAt(Instant.now());

        MessageCitation citation = new MessageCitation();
        citation.setMessage(message);
        citation.setLabel("Source 1");
        citation.setChunkId(UUID.randomUUID());
        citation.setDocumentId(UUID.randomUUID());
        citation.setLocator(Map.of("page", 1));

        Pageable pageable = PageRequest.of(0, 10);
        when(courseAccessClient.getAiAccess(courseId, "token")).thenReturn(fullAccess());
        when(conversationRepository.findByIdAndCourseIdAndUserIdAndDeletedAtIsNull(
                conversationId,
                courseId,
                userId
        )).thenReturn(Optional.of(conversation));
        when(messageRepository.readAllByConversationIdOrderByCreatedAtDesc(
                conversationId,
                pageable
        )).thenReturn(new PageImpl<>(List.of(message), pageable, 15));
        when(citationRepository.findAllByMessageIdIn(List.of(message.getId()))).thenReturn(List.of(citation));

        PagingResponse<ChatMessageResponse> response = conversationService.getMessages(
                courseId,
                conversationId,
                userId,
                "token",
                new PagingRequest()
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).citations()).hasSize(1);
        assertThat(response.getPageable().getTotalElements()).isEqualTo(15);
        assertThat(response.getPageable().getTotalPages()).isEqualTo(2);
        verify(messageRepository).readAllByConversationIdOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(conversationId),
                argThat(request -> request.getPageNumber() == 0 && request.getPageSize() == 10)
        );
    }

    private AiConversation conversation(UUID courseId, UUID userId) {
        AiConversation conversation = new AiConversation();
        conversation.setId(UUID.randomUUID());
        conversation.setCourseId(courseId);
        conversation.setUserId(userId);
        conversation.setTitle("Conversation");
        conversation.setLastMessageAt(Instant.now());
        conversation.setCreatedAt(Instant.now());
        return conversation;
    }

    private CoreCourseAccessClient.CourseAiAccess fullAccess() {
        return new CoreCourseAccessClient.CourseAiAccess(CoreCourseAccessClient.AccessLevel.FULL, null);
    }
}
