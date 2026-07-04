package com.inkpulse.corehelpers.exceptions;

public class BusinessValidationException extends BaseException {
    public BusinessValidationException(String message) {
        super(message, 422, "BUSINESS_VALIDATION_ERROR");
    }

    public BusinessValidationException(String message, String errorCode) {
        super(message, 422, errorCode);
    }
}
