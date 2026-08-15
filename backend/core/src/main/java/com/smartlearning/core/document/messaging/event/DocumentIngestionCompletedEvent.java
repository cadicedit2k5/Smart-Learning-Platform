package com.smartlearning.core.document.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DocumentIngestionCompletedEvent(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,

        UUID requestEventId,
        UUID processingJobId,
        UUID documentVersionId,

        int chunkCount,
        String modelKey
) {
}
