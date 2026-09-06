package com.smartlearning.core.infrastructure.http;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.infrastructure.dto.SystemUserResponse;
import com.smartlearning.core.infrastructure.dto.UserBatchLookupRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
public class SystemClient {

    private final RestClient systemRestClient;

    public SystemClient(
            @Qualifier("systemRestClient")
            RestClient systemRestClient) {
        this.systemRestClient = systemRestClient;
    }

    public List<SystemUserResponse> lookupUsers(
            Collection<UUID> userIds,
            String accessToken) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        try {
            List<SystemUserResponse> response = systemRestClient
                    .post().uri("/internal/users/lookup")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(new UserBatchLookupRequest(Set.copyOf(userIds)))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return response == null ? List.of() : response;

        } catch (RestClientException exception) {
            throw new ApplicationException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "Không thể lấy thông tin người dùng từ System Service",
                    exception
            );
        }
    }
}
