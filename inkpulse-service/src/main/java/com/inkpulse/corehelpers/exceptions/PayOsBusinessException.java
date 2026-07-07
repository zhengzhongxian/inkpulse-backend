package com.inkpulse.corehelpers.exceptions;

public class PayOsBusinessException extends BusinessValidationException {
    public PayOsBusinessException(String message, String errorCode) {
        super(message, errorCode);
    }
}
