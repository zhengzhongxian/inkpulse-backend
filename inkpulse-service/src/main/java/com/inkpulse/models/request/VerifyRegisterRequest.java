package com.inkpulse.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class VerifyRegisterRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @Size(min = 6, max = 6, message = "Mã OTP phải có đúng 6 ký tự")
    private String otpCode;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    private String userName;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
        message = "Mật khẩu phải bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt"
    )
    private String password;

    @NotBlank(message = "Tên không được để trống")
    private String firstName;

    @NotBlank(message = "Họ không được để trống")
    private String lastName;

    @NotBlank(message = "Giới tính không được để trống")
    private String gender;

    @NotNull(message = "Ngày sinh không được để trống")
    private LocalDate dob;

    @NotBlank(message = "Device ID không được để trống")
    private String deviceId;

    @NotBlank(message = "Tên thiết bị không được để trống")
    private String deviceName;

    @NotBlank(message = "Loại thiết bị không được để trống")
    private String deviceType;

    @NotBlank(message = "Fingerprint không được để trống")
    private String browserFingerprint;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    private String recipientPhone;

    @NotNull(message = "Tỉnh/Thành phố không được để trống")
    private Integer provinceId;

    @NotNull(message = "Quận/Huyện không được để trống")
    private Integer districtId;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String wardCode;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String streetAddress;

    private String addressLabel;

    private String choiceLanguage;

    public VerifyRegisterRequest() {}

    public VerifyRegisterRequest(
            String email, String otpCode, String userName, String password,
            String firstName, String lastName, String gender, LocalDate dob,
            String deviceId, String deviceName, String deviceType,
            String browserFingerprint, String choiceLanguage,
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
        this.choiceLanguage = choiceLanguage;
        this.recipientPhone = recipientPhone;
        this.provinceId = provinceId;
        this.districtId = districtId;
        this.wardCode = wardCode;
        this.streetAddress = streetAddress;
        this.addressLabel = addressLabel;
    }
}
