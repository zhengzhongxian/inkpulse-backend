package com.inkpulse.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNewDeviceAlertEmailMessage {
    private String email;
    private String deviceName;
    private String ipAddress;
}
