package com.acommon.exception;

import org.springframework.http.HttpStatus;

public class CommonException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public CommonException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public CommonException(String message, HttpStatus status) {
        this(message, status, "pointvent_ERROR");
    }

    public CommonException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
