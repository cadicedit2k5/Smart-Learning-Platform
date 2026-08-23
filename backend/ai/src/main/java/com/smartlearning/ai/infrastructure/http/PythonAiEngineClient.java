package com.smartlearning.ai.infrastructure.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartlearning.ai.infrastructure.dto.request.RagRequest;
import com.smartlearning.ai.infrastructure.dto.response.RagResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PythonAiEngineClient {

    private final RestClient aiEngineRestClient;

    public PythonAiEngineClient(
            @Qualifier("aiEngineRestClient")RestClient aiEngineRestClient
    ) {
        this.aiEngineRestClient = aiEngineRestClient;
    }

    public RagResult answer(UUID courseId, String question) {

        RagResponse response = aiEngineRestClient.post().uri("/internal/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new RagRequest(courseId, question))
                        .retrieve()
                        .body(RagResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "AI Engine returned empty response"
            );
        }

        return new RagResult(response.answer(),
            response.citations().stream().map(citation ->
                new Citation(
                        citation.label(),
                        citation.chunkId(),
                        citation.documentId(),
                        citation.documentVersionId(),
                        citation.locator()
                )).toList()
        );
    }


    public record RagResult(
            String answer,
            List<Citation> citations
    ) {
    }

    public record Citation(
            String label,
            UUID chunkId,
            UUID documentId,
            UUID documentVersionId,
            Map<String, Object> locator
    ) {
    }

}