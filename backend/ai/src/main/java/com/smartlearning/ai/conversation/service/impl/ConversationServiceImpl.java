package com.smartlearning.ai.conversation.service.impl;

import com.smartlearning.ai.conversation.config.ConversationProperties;
import com.smartlearning.ai.conversation.dto.response.ChatMessageResponse;
import com.smartlearning.ai.conversation.dto.response.ChatTurnResponse;
import com.smartlearning.ai.conversation.dto.response.ConversationSummaryResponse;
import com.smartlearning.ai.conversation.entity.AiConversation;
import com.smartlearning.ai.conversation.entity.ChatMessage;
import com.smartlearning.ai.conversation.entity.MessageCitation;
import com.smartlearning.ai.conversation.entity.enums.ChatAccessScope;
import com.smartlearning.ai.conversation.entity.enums.ChatMessageRole;
import com.smartlearning.ai.conversation.repository.AiConversationRepository;
import com.smartlearning.ai.conversation.repository.ChatMessageRepository;
import com.smartlearning.ai.conversation.repository.MessageCitationRepository;
import com.smartlearning.ai.conversation.service.ConversationService;
import com.smartlearning.ai.infrastructure.http.CoreCourseAccessClient;
import com.smartlearning.ai.infrastructure.http.PythonAiEngineClient;
import com.smartlearning.common.dto.request.PagingRequest;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private static final int TITLE_MAX_LENGTH = 80;

    private final ConversationProperties properties;
    private final AiConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final MessageCitationRepository citationRepository;
    private final CoreCourseAccessClient courseAccessClient;
    private final PythonAiEngineClient aiEngineClient;
    private final TransactionTemplate transactionTemplate;

    @Override
    public ChatTurnResponse startConversation(UUID courseId, UUID userId, String accessToken, String content) {
        String question = content.trim();

        CoreCourseAccessClient.CourseAiAccess access = courseAccessClient.getAiAccess(courseId, accessToken);
        ChatAccessScope scope = requireChatAccessScope(access);

        StartResult start = Objects.requireNonNull(transactionTemplate.execute(
                status -> createConversationAndUserMessage(courseId, userId, question, scope)
        ));

        PythonAiEngineClient.AiAnswer answer = executeAi(access, scope, courseId, List.of(), question);
        ChatMessageResponse assistant = saveAssistantMessage(start.conversationId(), courseId, userId, scope, answer);
        ChatMessageResponse userMessage = toUserMessageResponse(
                start.userMessageId(),
                scope,
                question,
                start.userCreatedAt()
        );

        return new ChatTurnResponse(start.conversationId(), userMessage, assistant);
    }

    @Override
    public ChatTurnResponse sendMessage(
            UUID courseId,
            UUID conversationId,
            UUID userId,
            String accessToken,
            String content
    ) {
        String question = content.trim();

        CoreCourseAccessClient.CourseAiAccess access = courseAccessClient.getAiAccess(courseId, accessToken);
        ChatAccessScope scope = requireChatAccessScope(access);

        requireConversation(conversationId, courseId, userId);

        // Load history before saving the current question to avoid sending it to the AI engine twice.
        List<ChatMessage> history = loadHistory(conversationId, scope);

        UserMessageResult user = Objects.requireNonNull(transactionTemplate.execute(
                status -> saveUserMessage(conversationId, courseId, userId, scope, question)
        ));

        PythonAiEngineClient.AiAnswer answer = executeAi(access, scope, courseId, history, question);
        ChatMessageResponse assistant = saveAssistantMessage(conversationId, courseId, userId, scope, answer);
        ChatMessageResponse userMessage = toUserMessageResponse(user.messageId(), scope, question, user.createdAt());

        return new ChatTurnResponse(conversationId, userMessage, assistant);
    }

    @Override
    public PagingResponse<ConversationSummaryResponse> getConversations(
            UUID courseId,
            UUID userId,
            String accessToken,
            PagingRequest pagingRequest
    ) {
        CoreCourseAccessClient.CourseAiAccess access = courseAccessClient.getAiAccess(courseId, accessToken);
        requireChatAccessScope(access);

        Page<ConversationSummaryResponse> conversations = conversationRepository
                .findAllByCourseIdAndUserIdAndDeletedAtIsNullOrderByLastMessageAtDesc(
                        courseId,
                        userId,
                        pagingRequest.pageable()
                )
                .map(this::toConversationSummaryResponse);

        return PagingResponse.from(conversations);
    }

    @Override
    public PagingResponse<ChatMessageResponse> getMessages(
            UUID courseId,
            UUID conversationId,
            UUID userId,
            String accessToken,
            PagingRequest pagingRequest
    ) {
        CoreCourseAccessClient.CourseAiAccess access = courseAccessClient.getAiAccess(courseId, accessToken);
        ChatAccessScope scope = requireChatAccessScope(access);

        requireConversation(conversationId, courseId, userId);

        Page<ChatMessageResponse> messages = toResponses(
                loadMessages(conversationId, scope, pagingRequest.pageable())
        );

        return PagingResponse.from(messages);
    }

    private ChatAccessScope requireChatAccessScope(CoreCourseAccessClient.CourseAiAccess access) {
        return switch (access.level()) {
            case FULL -> ChatAccessScope.FULL;
            case PREVIEW -> ChatAccessScope.PREVIEW;
            case DENIED -> throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        };
    }

    private List<ChatMessage> loadHistory(UUID conversationId, ChatAccessScope scope) {
        Pageable limit = PageRequest.of(0, properties.historyLimit());

        List<ChatMessage> messages;
        if (scope == ChatAccessScope.PREVIEW) {
            messages = messageRepository.findAllByConversation_IdAndAccessScopeOrderByCreatedAtDesc(
                    conversationId,
                    ChatAccessScope.PREVIEW,
                    limit
            );
        } else {
            messages = messageRepository.findAllByConversationIdOrderByCreatedAtDesc(conversationId, limit);
        }

        List<ChatMessage> chronological = new ArrayList<>(messages);
        Collections.reverse(chronological);
        return chronological;
    }

    private Page<ChatMessage> loadMessages(UUID conversationId, ChatAccessScope scope, Pageable pageable) {
        if (scope == ChatAccessScope.PREVIEW) {
            return messageRepository.readAllByConversationIdAndAccessScopeOrderByCreatedAtDesc(
                    conversationId,
                    ChatAccessScope.PREVIEW,
                    pageable
            );
        }

        return messageRepository.readAllByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
    }

    private PythonAiEngineClient.AiAnswer executeAi(
            CoreCourseAccessClient.CourseAiAccess access,
            ChatAccessScope scope,
            UUID courseId,
            List<ChatMessage> history,
            String question
    ) {
        List<PythonAiEngineClient.HistoryMessage> engineHistory = history.stream()
                .map(message -> new PythonAiEngineClient.HistoryMessage(message.getRole().name(), message.getContent()))
                .toList();

        return switch (scope) {
            case FULL -> aiEngineClient.answerWithRag(courseId, engineHistory, question);
            case PREVIEW -> aiEngineClient.answerPreview(
                    Objects.requireNonNull(access.preview()),
                    engineHistory,
                    question
            );
        };
    }

    private StartResult createConversationAndUserMessage(
            UUID courseId,
            UUID userId,
            String question,
            ChatAccessScope scope
    ) {
        AiConversation conversation = new AiConversation();
        conversation.setCourseId(courseId);
        conversation.setUserId(userId);
        conversation.setTitle(createTitle(question));
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.saveAndFlush(conversation);

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setRole(ChatMessageRole.USER);
        message.setAccessScope(scope);
        message.setContent(question);
        messageRepository.saveAndFlush(message);

        return new StartResult(conversation.getId(), message.getId(), message.getCreatedAt());
    }

    private UserMessageResult saveUserMessage(
            UUID conversationId,
            UUID courseId,
            UUID userId,
            ChatAccessScope scope,
            String content
    ) {
        AiConversation conversation = requireConversation(conversationId, courseId, userId);

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setRole(ChatMessageRole.USER);
        message.setAccessScope(scope);
        message.setContent(content);
        messageRepository.saveAndFlush(message);

        conversation.setLastMessageAt(Instant.now());

        return new UserMessageResult(message.getId(), message.getCreatedAt());
    }

    private ChatMessageResponse saveAssistantMessage(
            UUID conversationId,
            UUID courseId,
            UUID userId,
            ChatAccessScope scope,
            PythonAiEngineClient.AiAnswer answer
    ) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            AiConversation conversation = requireConversation(conversationId, courseId, userId);

            ChatMessage assistant = new ChatMessage();
            assistant.setConversation(conversation);
            assistant.setRole(ChatMessageRole.ASSISTANT);
            assistant.setAccessScope(scope);
            assistant.setContent(answer.answer());
            messageRepository.saveAndFlush(assistant);

            List<MessageCitation> citations = answer.citations()
                    .stream()
                    .map(citation -> toCitationEntity(citation, assistant))
                    .toList();

            citationRepository.saveAll(citations);
            conversation.setLastMessageAt(Instant.now());

            return new ChatMessageResponse(
                    assistant.getId(),
                    assistant.getRole().name(),
                    scope.name(),
                    assistant.getContent(),
                    answer.citations().stream().map(this::toResponseCitation).toList(),
                    assistant.getCreatedAt()
            );
        }));
    }

    private AiConversation requireConversation(UUID conversationId, UUID courseId, UUID userId) {
        return conversationRepository.findByIdAndCourseIdAndUserIdAndDeletedAtIsNull(
                        conversationId,
                        courseId,
                        userId
                )
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    private ChatMessageResponse toUserMessageResponse(
            UUID messageId,
            ChatAccessScope scope,
            String content,
            Instant createdAt
    ) {
        return new ChatMessageResponse(
                messageId,
                ChatMessageRole.USER.name(),
                scope.name(),
                content,
                List.of(),
                createdAt
        );
    }

    private Page<ChatMessageResponse> toResponses(Page<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return messages.map(message -> toResponse(message, List.of()));
        }

        List<UUID> messageIds = messages.getContent().stream().map(ChatMessage::getId).toList();
        Map<UUID, List<MessageCitation>> citationsByMessage = citationRepository.findAllByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.groupingBy(citation -> citation.getMessage().getId()));

        return messages.map(message ->
                toResponse(message, citationsByMessage.getOrDefault(message.getId(), List.of()))
        );
    }

    private ConversationSummaryResponse toConversationSummaryResponse(AiConversation conversation) {
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt()
        );
    }

    private ChatMessageResponse toResponse(ChatMessage message, List<MessageCitation> citations) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole().name(),
                message.getAccessScope().name(),
                message.getContent(),
                citations.stream().map(this::toResponseCitation).toList(),
                message.getCreatedAt()
        );
    }

    private MessageCitation toCitationEntity(PythonAiEngineClient.Citation citation, ChatMessage message) {
        MessageCitation entity = new MessageCitation();
        entity.setMessage(message);
        entity.setLabel(citation.label());
        entity.setChunkId(citation.chunkId());
        entity.setDocumentId(citation.documentId());
        entity.setDocumentVersionId(citation.documentVersionId());
        entity.setLocator(citation.locator());
        return entity;
    }

    private ChatMessageResponse.Citation toResponseCitation(PythonAiEngineClient.Citation citation) {
        return new ChatMessageResponse.Citation(
                citation.label(),
                citation.chunkId(),
                citation.documentId(),
                citation.documentVersionId(),
                citation.locator()
        );
    }

    private ChatMessageResponse.Citation toResponseCitation(MessageCitation citation) {
        return new ChatMessageResponse.Citation(
                citation.getLabel(),
                citation.getChunkId(),
                citation.getDocumentId(),
                citation.getDocumentVersionId(),
                citation.getLocator()
        );
    }

    private String createTitle(String content) {
        if (content.length() <= TITLE_MAX_LENGTH) {
            return content;
        }

        return content.substring(0, TITLE_MAX_LENGTH).trim() + "...";
    }

    private record StartResult(UUID conversationId, UUID userMessageId, Instant userCreatedAt) {
    }

    private record UserMessageResult(UUID messageId, Instant createdAt) {
    }
}
