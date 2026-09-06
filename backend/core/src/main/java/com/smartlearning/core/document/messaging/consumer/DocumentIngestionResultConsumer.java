package com.smartlearning.core.document.messaging.consumer;

import com.smartlearning.core.document.messaging.event.DocumentIngestionCompletedEvent;
import com.smartlearning.core.document.messaging.event.DocumentIngestionFailedEvent;
import com.smartlearning.core.document.messaging.kafka.DocumentKafkaTopics;
import com.smartlearning.core.document.service.DocumentProcessingResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionResultConsumer {

    private final JsonMapper jsonMapper;

    private final DocumentProcessingResultService resultService;

    @KafkaListener(
            topics = DocumentKafkaTopics.DOCUMENT_INGESTION_COMPLETED,
            groupId = "core-document-ingestion-status",
            ackMode = "RECORD"
    )
    public void consumeCompleted(String payload) throws JacksonException {

        DocumentIngestionCompletedEvent event =jsonMapper.readValue(
                payload,
                DocumentIngestionCompletedEvent.class);

        resultService.handleCompleted(event);

        log.info(
                "Document indexed: documentVersionId={}, chunks={}",
                event.documentVersionId(),
                event.chunkCount()
        );
    }

    @KafkaListener(
            topics = DocumentKafkaTopics.DOCUMENT_INGESTION_FAILED,
            groupId = "core-document-ingestion-status",
            ackMode = "RECORD"
    )
    public void consumeFailed(String payload) throws JacksonException {

        DocumentIngestionFailedEvent event = jsonMapper.readValue(
                        payload,
                        DocumentIngestionFailedEvent.class);

        resultService.handleFailed(event);

        log.warn(
                "Document ingestion failed: documentVersionId={}, error={}",
                event.documentVersionId(),
                event.errorMessage()
        );
    }
}