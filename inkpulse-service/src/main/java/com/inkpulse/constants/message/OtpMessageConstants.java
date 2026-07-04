package com.inkpulse.constants.message;

public final class OtpMessageConstants {

    private OtpMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String SENT = "Mã OTP đã được gửi thành công";
    public static final String EMAIL_LIMITED = "Vượt quá giới hạn nhận mã OTP của Email. Vui lòng thử lại sau.";
    public static final String EMAIL_DAILY_LIMITED = "Vượt quá giới hạn nhận mã OTP của Email trong ngày";
    public static final String DEVICE_LIMITED = "Vượt quá giới hạn nhận mã OTP của Thiết bị. Vui lòng thử lại sau.";
    public static final String DEVICE_HOURLY_LIMITED = "Vượt quá giới hạn nhận mã OTP của Thiết bị trong giờ";
    public static final String EMAIL_BLOCKED = "Email này đã bị tạm khoá nhận OTP. Vui lòng thử lại sau 24 giờ.";
    public static final String DEVICE_BLOCKED = "Thiết bị này đã bị tạm khoá nhận OTP. Vui lòng thử lại sau 24 giờ.";
    public static final String COOLDOWN_ACTIVE = "Vui lòng đợi trước khi yêu cầu mã OTP mới.";
}
