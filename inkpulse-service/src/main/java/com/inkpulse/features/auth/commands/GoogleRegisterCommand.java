package com.inkpulse.features.auth.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.auth.LoginResult;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleRegisterCommand implements Command<LoginResult> {
    private String googleUserId;
    private String email;
    private String name;
    private String picture;
    private String username;
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate dob;
    private String choiceLanguage;
    
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String browserFingerprint;
    private String clientIp;

    // Delivery address fields
    private String recipientPhone;
    private Integer provinceId;
    private Integer districtId;
    private String wardCode;
    private String streetAddress;
    private String addressLabel;
}
