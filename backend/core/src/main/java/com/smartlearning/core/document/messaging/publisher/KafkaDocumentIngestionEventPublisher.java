package com.smartlearning.core.document.messaging.publisher;

import com.smartlearning.core.document.messaging.event.DocumentIngestionRequestedEvent;
import com.smartlearning.core.document.messaging.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaDocumentIngestionEventPublisher implements DocumentIngestionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Override
    public void publish(DocumentIngestionRequestedEvent event) {
        String payload = jsonMapper.writeValueAsString(event);

        String messageKey = event.documentVersionId().toString();

        kafkaTemplate.send(KafkaTopics.DOCUMENT_INGESTION_REQUESTED,
                        messageKey,
                        payload)
                .whenComplete((result, exception) -> {

                    if (exception != null) {
                        log.error("Failed to publish document ingestion event: {}", event.eventId(), exception);
                        return;
                    }

                    log.info("Published document ingestion event: eventId={}, documentVersionId={}", event.eventId(),
                            event.documentVersionId());
                });
    }
}