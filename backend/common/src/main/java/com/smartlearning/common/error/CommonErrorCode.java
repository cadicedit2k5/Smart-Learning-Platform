package com.smartlearning.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    VALIDATION_FAILED(
            "VALIDATION_FAILED",
            HttpStatus.BAD_REQUEST,
            "Dữ liệu yêu cầu không hợp lệ"
    ),

    MALFORMED_REQUEST(
            "MALFORMED_REQUEST",
            HttpStatus.BAD_REQUEST,
            "Nội dung request không hợp lệ"
    ),

    RESOURCE_NOT_FOUND(
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            "Không tìm thấy tài nguyên"
    ),

    METHOD_NOT_ALLOWED(
            "METHOD_NOT_ALLOWED",
            HttpStatus.METHOD_NOT_ALLOWED,
            "Phương thức HTTP không được hỗ trợ"
    ),

    UNSUPPORTED_MEDIA_TYPE(
            "UNSUPPORTED_MEDIA_TYPE",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Kiểu dữ liệu request không được hỗ trợ"
    ),

    DATA_CONFLICT(
            "DATA_CONFLICT",
            HttpStatus.CONFLICT,
            "Dữ liệu bị xung đột"
    ),

    INTERNAL_SERVER_ERROR(
            "INTERNAL_SERVER_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Đã xảy ra lỗi hệ thống"
    ),
    UNAUTHORIZED(
            "UNAUTHORIZED",
            HttpStatus.UNAUTHORIZED,
            "Chưa xác thực hoặc token không hợp lệ"
    ),
    FORBIDDEN("FORBIDDEN",
            HttpStatus.FORBIDDEN,
            "Bạn không có quyền truy cập tài nguyên này"
    );

    private final String code;

    private final HttpStatus httpStatus;

    private final String defaultMessage;
}
