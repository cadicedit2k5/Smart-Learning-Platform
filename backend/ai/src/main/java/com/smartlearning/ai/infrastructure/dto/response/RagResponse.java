package com.smartlearning.ai.infrastructure.dto.response;

import java.util.List;

public record RagResponse(
        String answer,
        List<RagCitationResponse> citations
) {
}