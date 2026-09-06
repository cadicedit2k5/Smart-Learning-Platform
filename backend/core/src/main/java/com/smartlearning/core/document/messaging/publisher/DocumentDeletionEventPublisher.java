package com.smartlearning.core.document.messaging.publisher;

import com.smartlearning.core.document.messaging.event.DocumentDeletionRequestedEvent;

public interface DocumentDeletionEventPublisher {
    void publish(DocumentDeletionRequestedEvent event);
}
