package com.inkpulse.corehelpers.exceptions;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super(message, 401, "UNAUTHORIZED");
    }

    public UnauthorizedException(String message, String errorCode) {
        super(message, 401, errorCode);
    }
}
