package com.smartlearning.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseStatus {

    final Instant timestamp;
    int code;
    String message;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<ApiError> errors = new ArrayList<>();

//    @Builder.Default
//    private final List<ApiError> errors = List.of();

    public static ResponseStatus success(int code) {
        return ResponseStatus.builder()
                .timestamp(Instant.now())
                .code(code)
                .errors(List.of())
                .build();
    }

    public static ResponseStatus failure(
            int code,
            List<ApiError> errors
    ) {
        return ResponseStatus.builder()
                .timestamp(Instant.now())
                .code(code)
                .errors(errors == null
                        ? List.of()
                        : List.copyOf(errors))
                .build();
    }
}
