package com.inkpulse.corehelpers.exceptions;

public class MinioTechnicalException extends BaseException {
    public MinioTechnicalException(String errorCode, String message) {
        super(message, 500, errorCode);
    }
    public MinioTechnicalException(String errorCode, String message, Throwable cause) {
        super(message, 500, errorCode);
        initCause(cause);
    }
}
