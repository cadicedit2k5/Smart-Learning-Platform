package com.smartlearning.system.auth.error;

import com.smartlearning.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    EMAIL_ALREADY_EXISTS(
            "EMAIL_ALREADY_EXISTS",
            HttpStatus.CONFLICT,
            "Email đã được sử dụng"
    ),

    ACCOUNT_NOT_FOUND(
            "ACCOUNT_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            "Không tìm thấy tài khoản"
    ),

    INVALID_CREDENTIALS(
            "INVALID_CREDENTIALS",
            HttpStatus.UNAUTHORIZED,
            "Thông tin đăng nhập không hợp lệ"
    );

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
