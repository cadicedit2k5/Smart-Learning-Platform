package com.smartlearning.core.document.messaging.listener;

import com.smartlearning.core.document.messaging.event.DocumentDeletionRequestedEvent;
import com.smartlearning.core.document.messaging.publisher.DocumentDeletionEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DocumentDeletionEventListener {

    private final DocumentDeletionEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DocumentDeletionRequestedEvent event) {
        publisher.publish(event);
    }
}