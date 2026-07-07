package com.inkpulse.corehelpers.exceptions;

public class PayOsTechnicalException extends BaseException {
    public PayOsTechnicalException(String message) {
        super(message, 500, "PAYOS_TECHNICAL_ERROR");
    }
}
