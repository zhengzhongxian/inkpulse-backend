package com.inkpulse.models.request.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendOtpRequest {
    private String mfaSessionId;
    private String email;
}
