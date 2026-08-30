package com.smartlearning.core.course.messaging.publisher;

import com.smartlearning.core.course.messaging.event.TopicKnowledgeIndexRequestedEvent;
import com.smartlearning.core.course.messaging.kafka.CourseKafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaTopicKnowledgeIndexEventPublisher implements TopicKnowledgeIndexEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Override
    public void publish(TopicKnowledgeIndexRequestedEvent event) {
        String payload = jsonMapper.writeValueAsString(event);

        kafkaTemplate.send(CourseKafkaTopics.TOPIC_KNOWLEDGE_INDEX_REQUESTED, event.topicId().toString(), payload)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish topic knowledge event: {}", event.eventId(), exception);
                        return;
                    }

                    log.info("Published topic knowledge event: eventId={}, topicId={}", event.eventId(), event.topicId());
                });
    }
}