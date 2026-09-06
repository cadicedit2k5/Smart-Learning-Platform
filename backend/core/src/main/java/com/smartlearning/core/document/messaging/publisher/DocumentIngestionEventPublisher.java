package com.smartlearning.core.document.messaging.publisher;

import com.smartlearning.core.document.messaging.event.DocumentIngestionRequestedEvent;

public interface DocumentIngestionEventPublisher {

    void publish(DocumentIngestionRequestedEvent event);
}