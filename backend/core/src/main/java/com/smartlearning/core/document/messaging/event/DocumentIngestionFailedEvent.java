package com.smartlearning.core.document.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DocumentIngestionFailedEvent(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,

        UUID requestEventId,
        UUID processingJobId,
        UUID documentVersionId,

        String errorType,
        String errorMessage
) {
}
