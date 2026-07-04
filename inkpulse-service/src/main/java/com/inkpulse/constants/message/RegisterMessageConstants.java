package com.inkpulse.constants.message;

public final class RegisterMessageConstants {

    private RegisterMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String SUCCESS = "Đăng ký tài khoản thành công";
    public static final String USERNAME_TAKEN = "Tên đăng nhập đã được sử dụng";
    public static final String EMAIL_TAKEN = "Email đã được đăng ký trên hệ thống";
    public static final String OTP_INVALID = "Mã OTP không chính xác";
    public static final String OTP_EXPIRED = "Mã OTP đã hết hạn";
    public static final String OTP_BLOCKED_ATTEMPTS = "Nhập sai mã OTP quá nhiều lần. Gửi mã OTP đăng ký đã bị khoá cho email/thiết bị này.";
    public static final String INVALID_INPUT = "Thông tin đăng ký không hợp lệ";
}
