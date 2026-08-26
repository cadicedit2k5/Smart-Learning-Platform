package com.smartlearning.ai.infrastructure.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.UUID;

public record RagCitationResponse(
        String label,

        @JsonProperty("chunk_id")
        UUID chunkId,

        @JsonProperty("document_id")
        UUID documentId,

        @JsonProperty("document_version_id")
        UUID documentVersionId,

        Map<String, Object> locator
) {
}
