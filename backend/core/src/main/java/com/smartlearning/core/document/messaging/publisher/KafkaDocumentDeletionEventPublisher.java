package com.smartlearning.core.document.messaging.publisher;

import com.smartlearning.core.document.messaging.event.DocumentDeletionRequestedEvent;
import com.smartlearning.core.document.messaging.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaDocumentDeletionEventPublisher implements DocumentDeletionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Override
    public void publish(DocumentDeletionRequestedEvent event) {
        String payload = jsonMapper.writeValueAsString(event);

        kafkaTemplate.send(
                KafkaTopics.DOCUMENT_DELETION_REQUESTED,
                event.documentId().toString(),
                payload
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Failed to publish document deletion event: {}",
                        event.eventId(),
                        exception);
                return;
            }

            log.info(
                    "Published document deletion event: eventId={}, documentId={}",
                    event.eventId(),
                    event.documentId()
            );
        });
    }
}