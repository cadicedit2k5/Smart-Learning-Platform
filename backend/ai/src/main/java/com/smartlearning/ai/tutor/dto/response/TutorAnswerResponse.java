package com.smartlearning.ai.tutor.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TutorAnswerResponse(
        String answer,
        List<Citation> citations
) {

    public record Citation(
            String label,
            UUID chunkId,
            UUID documentId,
            UUID documentVersionId,
            Map<String, Object> locator
    ) {
    }
}
