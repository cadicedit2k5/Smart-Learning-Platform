package com.smartlearning.ai.support;

import com.smartlearning.ai.conversation.entity.AiConversation;
import com.smartlearning.ai.conversation.entity.ChatMessage;
import com.smartlearning.ai.conversation.entity.MessageCitation;
import com.smartlearning.ai.conversation.entity.enums.ChatAccessScope;
import com.smartlearning.ai.conversation.entity.enums.ChatMessageRole;
import com.smartlearning.ai.infrastructure.http.CoreCourseAccessClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AiTestData {

    public static final UUID COURSE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID CONVERSATION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    public static final UUID MESSAGE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final Instant TEST_TIME = Instant.parse("2026-08-22T12:00:00Z");

    private AiTestData() {
    }

    public static AiConversation conversation() {
        AiConversation conversation = new AiConversation();
        conversation.setId(CONVERSATION_ID);
        conversation.setCourseId(COURSE_ID);
        conversation.setUserId(USER_ID);
        conversation.setTitle("Conversation");
        conversation.setLastMessageAt(TEST_TIME);
        conversation.setCreatedAt(TEST_TIME);
        return conversation;
    }

    public static ChatMessage assistantMessage(AiConversation conversation, ChatAccessScope scope) {
        ChatMessage message = new ChatMessage();
        message.setId(MESSAGE_ID);
        message.setConversation(conversation);
        message.setRole(ChatMessageRole.ASSISTANT);
        message.setAccessScope(scope);
        message.setContent("Answer");
        message.setCreatedAt(TEST_TIME);
        return message;
    }

    public static MessageCitation citation(ChatMessage message) {
        MessageCitation citation = new MessageCitation();
        citation.setMessage(message);
        citation.setLabel("Source 1");
        citation.setChunkId(UUID.fromString("50000000-0000-0000-0000-000000000001"));
        citation.setDocumentId(UUID.fromString("60000000-0000-0000-0000-000000000001"));
        citation.setLocator(Map.of("page", 1));
        return citation;
    }

    public static CoreCourseAccessClient.CourseAiAccess fullAccess() {
        return new CoreCourseAccessClient.CourseAiAccess(
                CoreCourseAccessClient.AccessLevel.FULL,
                null
        );
    }

    public static CoreCourseAccessClient.CourseAiAccess previewAccess() {
        CoreCourseAccessClient.CoursePreview preview = new CoreCourseAccessClient.CoursePreview(
                COURSE_ID,
                "Smart Learning",
                "Course description",
                "BEGINNER",
                List.of()
        );
        return new CoreCourseAccessClient.CourseAiAccess(
                CoreCourseAccessClient.AccessLevel.PREVIEW,
                preview
        );
    }
}
