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
import java.util.Objects;
import java.util.UUID;

@Component
public class PythonAiEngineClient {

    private final RestClient aiEngineRestClient;

    public PythonAiEngineClient(
            @Qualifier("aiEngineRestClient")RestClient aiEngineRestClient
    ) {
        this.aiEngineRestClient = aiEngineRestClient;
    }

    public AiAnswer answerWithRag(UUID courseId, List<HistoryMessage> history, String question) {

        RagResponse response = aiEngineRestClient.post().uri("/internal/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new RagRequest(courseId, history, question))
                        .retrieve()
                        .body(RagResponse.class);

        if (response == null) {
            throw new IllegalStateException("AI Engine returned empty response");
        }

        return new AiAnswer(response.answer(),
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

    public AiAnswer answerPreview(
            CoreCourseAccessClient.CoursePreview course,
            List<HistoryMessage> history,
            String question) {

        PreviewResponse response = aiEngineRestClient.post().uri("/internal/course-preview/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new PreviewRequest(course, history, question))
                        .retrieve()
                        .body(PreviewResponse.class);

        response = Objects.requireNonNull(response,
                "AI Engine returned empty preview response");

        return new AiAnswer(response.answer(), List.of());
    }


    private record RagRequest(
            @JsonProperty("course_id")
            UUID courseId,
            List<HistoryMessage> history,
            String question
    ) {}


    private record PreviewRequest(
            CoreCourseAccessClient.CoursePreview course,
            List<HistoryMessage> history,
            String question
    ) {}


    private record PreviewResponse(
            String answer
    ) {}


    private record RagResponse(
            String answer,
            List<RagCitationResponse> citations
    ) {}


    private record RagCitationResponse(
            String label,
            @JsonProperty("chunk_id")
            UUID chunkId,
            @JsonProperty("document_id")
            UUID documentId,
            @JsonProperty("document_version_id")
            UUID documentVersionId,
            Map<String, Object> locator
    ) {}


    public record HistoryMessage(
            String role,
            String content
    ) {}


    public record AiAnswer(
            String answer,
            List<Citation> citations
    ) {}

    public record Citation(
            String label,
            UUID chunkId,
            UUID documentId,
            UUID documentVersionId,
            Map<String, Object> locator
    ) {}
}