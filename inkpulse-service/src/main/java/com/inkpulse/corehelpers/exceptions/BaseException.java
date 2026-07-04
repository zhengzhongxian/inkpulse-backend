package com.inkpulse.corehelpers.exceptions;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

    protected BaseException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
}
