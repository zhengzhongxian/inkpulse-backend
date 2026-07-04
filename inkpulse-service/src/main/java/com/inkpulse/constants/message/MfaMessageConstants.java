package com.inkpulse.constants.message;

public final class MfaMessageConstants {

    private MfaMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String INVALID_SESSION = "Phiên xác thực MFA đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.";
    public static final String INVALID_CODE = "Mã xác thực không chính xác";
    public static final String MAX_ATTEMPTS = "Vượt quá số lần xác thực MFA cho phép. Vui lòng đăng nhập lại.";
    public static final String VERIFIED = "Xác thực hai lớp (MFA) thành công";
}
