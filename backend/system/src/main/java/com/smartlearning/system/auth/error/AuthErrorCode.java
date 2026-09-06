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
    ),

    ROLE_NOT_FOUND(
        "ROLE_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Không tìm thấy role"
    ),

    ROLE_ALREADY_EXISTS(
        "ROLE_ALREADY_EXISTS",
        HttpStatus.CONFLICT,
        "Mã role đã tồn tại"
    ),

    ROLE_IN_USE(
        "ROLE_IN_USE",
        HttpStatus.CONFLICT,
        "Role đang được sử dụng và không thể xóa"
    ),

    SYSTEM_ROLE_PROTECTED(
        "SYSTEM_ROLE_PROTECTED",
        HttpStatus.CONFLICT,
        "Role hệ thống không thể xóa"
    ),

    ADMIN_PERMISSIONS_PROTECTED(
        "ADMIN_PERMISSIONS_PROTECTED",
        HttpStatus.CONFLICT,
        "Permission của role ADMIN được hệ thống quản lý"
    ),

    PERMISSION_NOT_FOUND(
        "PERMISSION_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Có permission không tồn tại"
    );

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
