package com.inkpulse.corehelpers.exceptions;

public class MinioBusinessException extends BaseException {
    public MinioBusinessException(String errorCode, String message) {
        super(message, 400, errorCode);
    }
    public MinioBusinessException(String errorCode, String message, Throwable cause) {
        super(message, 400, errorCode);
        initCause(cause);
    }
}
