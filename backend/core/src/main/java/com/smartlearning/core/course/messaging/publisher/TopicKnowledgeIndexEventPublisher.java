package com.smartlearning.core.course.messaging.publisher;


import com.smartlearning.core.course.messaging.event.TopicKnowledgeIndexRequestedEvent;

public interface TopicKnowledgeIndexEventPublisher {
    void publish(TopicKnowledgeIndexRequestedEvent event);
}
