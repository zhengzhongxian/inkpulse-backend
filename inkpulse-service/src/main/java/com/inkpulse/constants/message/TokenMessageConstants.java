package com.inkpulse.constants.message;

public final class TokenMessageConstants {

    private TokenMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String REFRESHED = "Làm mới mã thông báo thành công";
    public static final String REVOKED = "Thu hồi mã thông báo thành công";
    public static final String REUSE_DETECTED = "Phát hiện mã thông báo làm mới được sử dụng lại. Toàn bộ mã thông báo đã bị thu hồi vì lý do bảo mật.";
    public static final String INVALID = "Mã thông báo làm mới không hợp lệ hoặc đã hết hạn";
}
