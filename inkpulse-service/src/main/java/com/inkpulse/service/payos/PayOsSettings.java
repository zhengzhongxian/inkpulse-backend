package com.inkpulse.service.payos;

public interface PayOsSettings {
    String getReturnUrl();
    String getCancelUrl();
    int getExpiryMinutes();
}
