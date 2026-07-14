package com.smartlearning.common.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Builder
public class ApiResponse<T> {

    private ResponseStatus status;
    private T data;

    public static <T> ApiResponse<T> success(
            HttpStatus httpStatus,
            T data
    ) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.success(httpStatus.value()))
                .data(data)
                .build();
    }

    public static ApiResponse<Void> failure(
            HttpStatus httpStatus,
            List<ApiError> errors
    ) {
        return ApiResponse.<Void>builder()
                .status(
                        ResponseStatus.failure(
                                httpStatus.value(),
                                errors
                        )
                )
                .data(null)
                .build();
    }
}