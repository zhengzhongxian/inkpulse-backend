package com.inkpulse.corehelpers.exceptions;

public class TokenRefreshException extends BaseException {
    public TokenRefreshException(String message, int statusCode, String errorCode) {
        super(message, statusCode, errorCode);
    }
}
