package com.smartlearning.ai.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(

        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        @Size(max = 10_000)
        String content
) {
}