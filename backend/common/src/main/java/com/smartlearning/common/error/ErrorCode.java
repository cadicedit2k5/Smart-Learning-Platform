package com.smartlearning.common.error;


import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String getCode();

    HttpStatus getHttpStatus();

    String getDefaultMessage();
}