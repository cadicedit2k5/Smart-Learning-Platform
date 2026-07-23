package com.smartlearning.storage.error;

import com.smartlearning.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StorageErrorCode implements ErrorCode {
    EMPTY_FILE(
            "STORAGE_EMPTY_FILE",
            HttpStatus.BAD_REQUEST,
            "File không được để trống"
    ),
    OBJECT_NOT_FOUND(
            "STORAGE_OBJECT_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            "Không tìm thấy file"
    ),
    OPERATION_FAILED(
            "STORAGE_OPERATION_FAILED",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Thao tác lưu trữ file thất bại"
    );

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}