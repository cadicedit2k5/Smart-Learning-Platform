package com.smartlearning.core.document.messaging.kafka;

public final class DocumentKafkaTopics {

    private DocumentKafkaTopics() {
    }

    public static final String DOCUMENT_INGESTION_REQUESTED =
            "document.ingestion.requested";

    public static final String DOCUMENT_INGESTION_COMPLETED =
            "document.ingestion.completed";

    public static final String DOCUMENT_INGESTION_FAILED =
            "document.ingestion.failed";

    public static final String DOCUMENT_DELETION_REQUESTED =
            "document.deletion.requested";
}