package com.smartlearning.core.course.messaging.listener;

import com.smartlearning.core.course.messaging.event.TopicKnowledgeIndexRequestedEvent;
import com.smartlearning.core.course.messaging.publisher.TopicKnowledgeIndexEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TopicKnowledgeIndexEventListener {

    private final TopicKnowledgeIndexEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TopicKnowledgeIndexRequestedEvent event) {
        publisher.publish(event);
    }
}