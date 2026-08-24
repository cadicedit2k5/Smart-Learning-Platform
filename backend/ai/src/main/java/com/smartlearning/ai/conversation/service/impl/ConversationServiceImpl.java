package com.smartlearning.ai.conversation.service.impl;


import com.smartlearning.ai.conversation.dto.response.ChatMessageResponse;
import com.smartlearning.ai.conversation.dto.response.ChatTurnResponse;
import com.smartlearning.ai.conversation.entity.AiConversation;
import com.smartlearning.ai.conversation.entity.ChatMessage;
import com.smartlearning.ai.conversation.entity.MessageCitation;
import com.smartlearning.ai.conversation.entity.enums.ChatMessageRole;
import com.smartlearning.ai.conversation.repository.AiConversationRepository;
import com.smartlearning.ai.conversation.repository.ChatMessageRepository;
import com.smartlearning.ai.conversation.repository.MessageCitationRepository;
import com.smartlearning.ai.conversation.service.ConversationService;
import com.smartlearning.ai.infrastructure.http.CoreCourseAccessClient;
import com.smartlearning.ai.infrastructure.http.PythonAiEngineClient;
import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final AiConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final MessageCitationRepository citationRepository;

    private final CoreCourseAccessClient courseAccessClient;
    private final PythonAiEngineClient aiEngineClient;

    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;

    public ChatTurnResponse startConversation(UUID courseId, UUID userId, String accessToken, String content) {

        courseAccessClient.requireActiveMember(courseId, accessToken);

        String normalizedContent = content.trim();

        StartResult startResult = transactionTemplate.execute(status -> {

            AiConversation conversation = new AiConversation();

            conversation.setCourseId(courseId);
            conversation.setUserId(userId);
            conversation.setTitle(createTitle(normalizedContent));
            conversation.setLastMessageAt(Instant.now());

            conversationRepository.saveAndFlush(conversation);

            ChatMessage userMessage = new ChatMessage();

            userMessage.setConversation(conversation);
            userMessage.setRole(ChatMessageRole.USER);
            userMessage.setContent(normalizedContent);

            messageRepository.saveAndFlush(userMessage);

            return new StartResult(
                conversation.getId(),
                userMessage.getId(),
                userMessage.getCreatedAt()
            );
        });

        startResult = Objects.requireNonNull(startResult);

        PythonAiEngineClient.RagResult result = aiEngineClient.answer(courseId, normalizedContent);
        ChatMessageResponse assistantResponse = transactionTemplate.execute(status -> {

                    AiConversation conversation = requireConversation(
                            startResult.conversationId(),
                            courseId,
                            userId);

                    ChatMessage assistant = new ChatMessage();
                    assistant.setConversation(conversation);
                    assistant.setRole(ChatMessageRole.ASSISTANT);
                    assistant.setContent(result.answer());

                    messageRepository.saveAndFlush(assistant);

                    List<ChatMessageResponse.Citation> responseCitations = new ArrayList<>();

                    for (PythonAiEngineClient.Citation citation : result.citations()) {

                        MessageCitation entity = new MessageCitation();

                        entity.setMessage(assistant);
                        entity.setLabel(citation.label());
                        entity.setChunkId(citation.chunkId());
                        entity.setDocumentId(citation.documentId());
                        entity.setDocumentVersionId(citation.documentVersionId());

                        if (citation.locator() != null) {
                            entity.setLocatorJson(jsonMapper.writeValueAsString(citation.locator()));
                        }

                        citationRepository.save(entity);

                        responseCitations.add(
                            new ChatMessageResponse.Citation(
                                    citation.label(),
                                    citation.chunkId(),
                                    citation.documentId(),
                                    citation.documentVersionId(),
                                    citation.locator()
                            )
                        );
                    }
                    conversation.setLastMessageAt(Instant.now());

                    return new ChatMessageResponse(
                            assistant.getId(),
                            assistant.getRole().name(),
                            assistant.getContent(),
                            responseCitations,
                            assistant.getCreatedAt()
                    );
                });
        return new ChatTurnResponse(
                startResult.conversationId(),

                new ChatMessageResponse(
                        startResult.userMessageId(),
                        ChatMessageRole.USER.name(),
                        normalizedContent,
                        List.of(),
                        startResult.userCreatedAt()
                ),

                Objects.requireNonNull(
                        assistantResponse
                )
        );
    }

    private record StartResult(
            UUID conversationId,
            UUID userMessageId,
            Instant userCreatedAt
    ) {
    }

    private String createTitle(
            String content
    ) {

        int maxLength = 80;

        if (content.length() <= maxLength) {
            return content;
        }

        return content.substring(
                0,
                maxLength
        ).trim() + "...";
    }

    private AiConversation requireConversation(
            UUID conversationId,
            UUID courseId,
            UUID userId
    ) {

        return conversationRepository
                .findByIdAndCourseIdAndUserIdAndDeletedAtIsNull(
                        conversationId,
                        courseId,
                        userId
                )
                .orElseThrow(
                        () -> new ApplicationException(
                                CommonErrorCode.RESOURCE_NOT_FOUND
                        )
                );
    }
}