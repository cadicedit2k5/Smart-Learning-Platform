package com.smartlearning.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private ResponseStatus status;
    private T data;

    public static <T> ApiResponse<T> success(
            HttpStatus httpStatus,
            T data,
            String message
    ) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.success(httpStatus.value(), message))
                .data(data)
                .build();
    }

    public static ApiResponse<Void> failure(
            HttpStatus httpStatus,
            List<ApiError> errors,
            String message
    ) {
        return ApiResponse.<Void>builder()
                .status(
                        ResponseStatus.failure(
                                httpStatus.value(),
                                errors,
                                message
                        )
                )
                .data(null)
                .build();
    }
}