package com.smartlearning.ai.conversation.service.impl;

import com.smartlearning.ai.conversation.config.ConversationProperties;
import com.smartlearning.ai.conversation.dto.response.ChatMessageResponse;
import com.smartlearning.ai.conversation.dto.response.ConversationSummaryResponse;
import com.smartlearning.ai.conversation.entity.AiConversation;
import com.smartlearning.ai.conversation.entity.ChatMessage;
import com.smartlearning.ai.conversation.entity.MessageCitation;
import com.smartlearning.ai.conversation.entity.enums.ChatAccessScope;
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

import java.util.List;
import java.util.Optional;

import static com.smartlearning.ai.support.AiTestData.CONVERSATION_ID;
import static com.smartlearning.ai.support.AiTestData.COURSE_ID;
import static com.smartlearning.ai.support.AiTestData.USER_ID;
import static com.smartlearning.ai.support.AiTestData.assistantMessage;
import static com.smartlearning.ai.support.AiTestData.citation;
import static com.smartlearning.ai.support.AiTestData.conversation;
import static com.smartlearning.ai.support.AiTestData.fullAccess;
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
        AiConversation conversation = conversation();
        Pageable pageable = PageRequest.of(0, 5);

        when(courseAccessClient.getAiAccess(COURSE_ID, "token")).thenReturn(fullAccess());
        when(conversationRepository.findAllByCourseIdAndUserIdAndDeletedAtIsNullOrderByLastMessageAtDesc(
                COURSE_ID,
                USER_ID,
                pageable
        )).thenReturn(new PageImpl<>(List.of(conversation), pageable, 12));

        PagingResponse<ConversationSummaryResponse> response = conversationService.getConversations(
                COURSE_ID,
                USER_ID,
                "token",
                new PagingRequest()
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).id()).isEqualTo(conversation.getId());
        assertThat(response.getPageable().getTotalElements()).isEqualTo(12);
        assertThat(response.getPageable().getTotalPages()).isEqualTo(3);
    }

    @Test
    void paginatesMessagesAndMapsCitations() {
        AiConversation conversation = conversation();
        ChatMessage message = assistantMessage(conversation, ChatAccessScope.FULL);
        MessageCitation citation = citation(message);

        Pageable pageable = PageRequest.of(0, 5);
        when(courseAccessClient.getAiAccess(COURSE_ID, "token")).thenReturn(fullAccess());
        when(conversationRepository.findByIdAndCourseIdAndUserIdAndDeletedAtIsNull(
                CONVERSATION_ID,
                COURSE_ID,
                USER_ID
        )).thenReturn(Optional.of(conversation));
        when(messageRepository.readAllByConversationIdOrderByCreatedAtDesc(
                CONVERSATION_ID,
                pageable
        )).thenReturn(new PageImpl<>(List.of(message), pageable, 15));
        when(citationRepository.findAllByMessageIdIn(List.of(message.getId()))).thenReturn(List.of(citation));

        PagingResponse<ChatMessageResponse> response = conversationService.getMessages(
                COURSE_ID,
                CONVERSATION_ID,
                USER_ID,
                "token",
                new PagingRequest()
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).citations()).hasSize(1);
        assertThat(response.getPageable().getTotalElements()).isEqualTo(15);
        assertThat(response.getPageable().getTotalPages()).isEqualTo(3);
        verify(messageRepository).readAllByConversationIdOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(CONVERSATION_ID),
                argThat(request -> request.getPageNumber() == 0 && request.getPageSize() == 5)
        );
    }
}
