package com.smartlearning.ai.conversation.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.conversation")
public record ConversationProperties(
        @Min(1) int historyLimit
) {
}
