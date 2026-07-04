package com.inkpulse.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNumberChallengeEmailMessage {
    private String email;
    private String subject;
    private int challengeNumber;
    private List<Integer> options;
    private String sessionId;
}
