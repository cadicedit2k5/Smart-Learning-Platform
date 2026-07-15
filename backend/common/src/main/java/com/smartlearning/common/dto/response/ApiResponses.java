package com.smartlearning.common.dto.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;

public final class ApiResponses {

    private ApiResponses() {
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return success(HttpStatus.OK, data);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return success(HttpStatus.CREATED, data);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(
            URI location,
            T data
    ) {
        return ResponseEntity
                .created(location)
                .body(ApiResponse.success(
                        HttpStatus.CREATED,
                        data,
                        "Created Successfully!"
                ));
    }

    public static <T> ResponseEntity<ApiResponse<T>> accepted(T data) {
        return success(HttpStatus.ACCEPTED, data);
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(
            HttpStatus httpStatus,
            T data
    ) {
        return ResponseEntity
                .status(httpStatus)
                .body(ApiResponse.success(httpStatus, data, "Success"));
    }

    public static ResponseEntity<ApiResponse<Void>> fail(
            HttpStatus httpStatus,
            ApiError error,
            String message
    ) {
        return fail(httpStatus, List.of(error), message);
    }

    public static ResponseEntity<ApiResponse<Void>> fail(
            HttpStatus httpStatus,
            List<ApiError> errors,
            String message
    ) {
        return ResponseEntity
                .status(httpStatus)
                .body(ApiResponse.failure(
                        httpStatus,
                        errors,
                        message
                ));
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity
                .noContent()
                .build();
    }
}