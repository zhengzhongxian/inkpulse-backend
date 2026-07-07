package com.inkpulse.models.request.auth;

import com.inkpulse.constants.message.AuthMessageConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class GoogleRegisterRequest {
    @NotBlank(message = AuthMessageConstants.GOOGLE_USER_ID_NOT_BLANK)
    private String googleUserId;

    @NotBlank(message = AuthMessageConstants.EMAIL_NOT_BLANK)
    private String email;

    @NotBlank(message = AuthMessageConstants.NAME_NOT_BLANK)
    private String name;

    private String picture;

    @NotBlank(message = AuthMessageConstants.USERNAME_NOT_BLANK)
    private String username;

    private String firstName;
    private String lastName;

    @NotBlank(message = AuthMessageConstants.GENDER_NOT_BLANK)
    private String gender;

    @NotNull(message = AuthMessageConstants.DOB_NOT_NULL)
    private LocalDate dob;

    @NotBlank(message = AuthMessageConstants.LANGUAGE_NOT_BLANK)
    private String choiceLanguage;

    @NotBlank(message = AuthMessageConstants.DEVICE_ID_NOT_BLANK)
    private String deviceId;

    @NotBlank(message = AuthMessageConstants.DEVICE_NAME_NOT_BLANK)
    private String deviceName;

    @NotBlank(message = AuthMessageConstants.DEVICE_TYPE_NOT_BLANK)
    private String deviceType;

    @NotBlank(message = AuthMessageConstants.FINGERPRINT_NOT_BLANK)
    private String browserFingerprint;

    // Delivery address fields
    @NotBlank(message = AuthMessageConstants.RECIPIENT_PHONE_NOT_BLANK)
    private String recipientPhone;

    @NotNull(message = AuthMessageConstants.PROVINCE_NOT_NULL)
    private Integer provinceId;

    @NotNull(message = AuthMessageConstants.DISTRICT_NOT_NULL)
    private Integer districtId;

    @NotBlank(message = AuthMessageConstants.WARD_NOT_BLANK)
    private String wardCode;

    @NotBlank(message = AuthMessageConstants.STREET_NOT_BLANK)
    private String streetAddress;

    private String addressLabel;
}
