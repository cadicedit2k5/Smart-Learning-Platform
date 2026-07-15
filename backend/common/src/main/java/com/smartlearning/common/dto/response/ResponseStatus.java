package com.smartlearning.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(
    level = AccessLevel.PRIVATE,
    makeFinal = true
)
public class ResponseStatus {

    Instant timestamp;
    int statusCode;
    String message;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Builder.Default
    List<ApiError> errors = List.of();

    public static ResponseStatus success(int code, String message) {
        return ResponseStatus.builder()
                .timestamp(Instant.now())
                .statusCode(code)
                .message(message)
                .errors(List.of())
                .build();
    }

    public static ResponseStatus failure(
            int code,
            List<ApiError> errors,
            String message
    ) {
        return ResponseStatus.builder()
                .timestamp(Instant.now())
                .statusCode(code)
                .message(message)
                .errors(errors == null
                        ? List.of()
                        : List.copyOf(errors))
                .build();
    }
}
