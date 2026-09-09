package com.smartlearning.core.notification.dto.response;

import com.smartlearning.core.notification.entity.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        UUID courseId,
        boolean read,
        Instant createdAt
) {
}