package com.inkpulse.features.auth.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.LoginResult;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class VerifyRegisterCommand implements Command<LoginResult> {
    private final String email;
    private final String otpCode;
    private final String userName;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final String gender;
    private final LocalDate dob;
    private final String deviceId;
    private final String deviceName;
    private final String deviceType;
    private final String browserFingerprint;
    private final String clientIp;
    private final String choiceLanguage;
    private final String recipientPhone;
    private final Integer provinceId;
    private final Integer districtId;
    private final String wardCode;
    private final String streetAddress;
    private final String addressLabel;

    public VerifyRegisterCommand(
            String email, String otpCode, String userName, String password,
            String firstName, String lastName, String gender, LocalDate dob,
            String deviceId, String deviceName, String deviceType,
            String browserFingerprint, String clientIp, String choiceLanguage,
            String recipientPhone, Integer provinceId, Integer districtId,
            String wardCode, String streetAddress, String addressLabel) {
        this.email = email;
        this.otpCode = otpCode;
        this.userName = userName;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.dob = dob;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.browserFingerprint = browserFingerprint;
        this.clientIp = clientIp;
        this.choiceLanguage = choiceLanguage;
        this.recipientPhone = recipientPhone;
        this.provinceId = provinceId;
        this.districtId = districtId;
        this.wardCode = wardCode;
        this.streetAddress = streetAddress;
        this.addressLabel = addressLabel;
    }
}
