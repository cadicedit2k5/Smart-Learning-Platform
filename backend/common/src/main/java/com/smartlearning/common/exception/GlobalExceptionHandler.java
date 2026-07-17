package com.smartlearning.common.exception;

import com.smartlearning.common.dto.response.ApiError;
import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleApplicationException(
            ApplicationException exception
    ) {
        ApiError error = ApiError.builder()
                .code(exception.getErrorCode().getCode())
                .message(exception.getMessage())
                .build();

        return ApiResponses.fail(
                exception.getErrorCode().getHttpStatus(),
                error,
                exception.getMessage()
        );
    }

    // Use for RequestBody exception
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        List<ApiError> errors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> ApiError.builder()
                        .code("VALIDATION_FAILED")
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .toList();

        return ApiResponses.fail(
                HttpStatus.BAD_REQUEST,
                errors,
                CommonErrorCode.VALIDATION_FAILED.getDefaultMessage()
        );
    }

    // Use for ModelAttribute exception
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            BindException exception
    ) {
        List<ApiError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> ApiError.builder()
                        .code("VALIDATION_FAILED")
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .toList();

        return ApiResponses.fail(
                HttpStatus.BAD_REQUEST,
                errors,
                CommonErrorCode.VALIDATION_FAILED.getDefaultMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception
    ) {
        log.error("Unexpected error", exception);

        ApiError error = ApiError.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("Đã xảy ra lỗi hệ thống")
                .build();

        return ApiResponses.fail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                error,
                CommonErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()
        );
    }
}