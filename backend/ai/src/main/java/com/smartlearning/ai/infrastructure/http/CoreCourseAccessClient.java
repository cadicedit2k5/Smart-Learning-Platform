package com.smartlearning.ai.infrastructure.http;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class CoreCourseAccessClient {

    private final RestClient coreRestClient;

    public CoreCourseAccessClient(
            @Qualifier("coreRestClient")
            RestClient coreRestClient) {
        this.coreRestClient = coreRestClient;
    }

    public CourseAiAccess getAiAccess(UUID courseId, String accessToken) {
        try {
            CourseAiAccessResponse response = coreRestClient.get()
                            .uri("/internal/courses/{courseId}/ai-access", courseId)
                            .header(HttpHeaders.AUTHORIZATION,
                                    "Bearer " + accessToken)
                            .retrieve()
                            .body(CourseAiAccessResponse.class);

            response = Objects.requireNonNull(response, "Core returned empty AI access response");

            return new CourseAiAccess(response.accessLevel(), response.preview());

        } catch (HttpClientErrorException.NotFound exception) {
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden exception) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }
    }

    public enum AccessLevel {
        DENIED,
        PREVIEW,
        FULL
    }

    public record CourseAiAccess(
            AccessLevel level,
            CoursePreview preview) {}

    private record CourseAiAccessResponse(
            AccessLevel accessLevel,
            CoursePreview preview
    ) {}

    public record CoursePreview(
            UUID courseId,
            String title,
            String description,
            String level,
            List<ChapterPreview> chapters
    ) {}

    public record ChapterPreview(
            UUID chapterId,
            String title,
            String description,
            String learningObjectives,
            Integer orderIndex,
            List<TopicPreview> topics
    ) {}

    public record TopicPreview(
            UUID topicId,
            String title,
            Integer orderIndex,
            Integer estimatedMinutes
    ) {}
}