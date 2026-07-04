package com.inkpulse.corehelpers.exceptions;

public class TooManyRequestException extends BaseException {
    public TooManyRequestException(String message) {
        super(message, 429, "TOO_MANY_REQUESTS");
    }

    public TooManyRequestException(String message, String errorCode) {
        super(message, 429, errorCode);
    }
}
