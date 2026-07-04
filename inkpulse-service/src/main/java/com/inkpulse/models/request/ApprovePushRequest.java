package com.inkpulse.models.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovePushRequest {
    private String mfaSessionId;
}
