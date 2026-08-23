package com.smartlearning.core.document.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DocumentIngestionRequestedEvent(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,

        UUID processingJobId,

        UUID courseId,
        UUID documentId,
        UUID documentVersionId,

        String storageBucket,
        String storageKey,
        String fileName,
        String mimeType
) {
}