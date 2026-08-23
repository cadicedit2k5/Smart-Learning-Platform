package com.smartlearning.core.document.messaging.kafka;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String DOCUMENT_INGESTION_REQUESTED =
            "document.ingestion.requested";

    public static final String DOCUMENT_INGESTION_COMPLETED =
            "document.ingestion.completed";

    public static final String DOCUMENT_INGESTION_FAILED =
            "document.ingestion.failed";
}