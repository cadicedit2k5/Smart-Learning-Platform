package com.smartlearning.core.document.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DocumentDeletionRequestedEvent(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        UUID courseId,
        UUID documentId
) {
}