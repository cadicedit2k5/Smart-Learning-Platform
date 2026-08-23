package com.smartlearning.ai.infrastructure.http;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class CoreCourseAccessClient {

    private final RestClient coreRestClient;

    public CoreCourseAccessClient(
            @Qualifier("coreRestClient")
            RestClient coreRestClient
    ) {
        this.coreRestClient = coreRestClient;
    }

    public void requireActiveMember(UUID courseId, String accessToken) {
        try {
            coreRestClient.get().uri("/internal/courses/{courseId}/access", courseId)
                    .header(HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.Forbidden exception) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        } catch (HttpClientErrorException.NotFound exception) {

            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}