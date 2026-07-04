package com.inkpulse.constants.message;

public final class AuthMessageConstants {

    private AuthMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String LOGIN_SUCCESS = "Đăng nhập thành công";
    public static final String LOGIN_INVALID_CREDENTIALS = "Tên đăng nhập/email hoặc mật khẩu không chính xác";
    public static final String LOGIN_ACCOUNT_LOCKED = "Tài khoản đã bị khoá do đăng nhập sai nhiều lần. Vui lòng thử lại sau.";
    public static final String LOGIN_ACCOUNT_DISABLED = "Tài khoản chưa được kích hoạt. Vui lòng xác thực email hoặc liên hệ hỗ trợ.";
    public static final String LOGIN_ACCOUNT_NOT_VERIFIED = "Email tài khoản chưa được xác thực. Vui lòng kiểm tra hộp thư.";
    public static final String LOGIN_MFA_REQUIRED = "Yêu cầu xác thực hai lớp (MFA)";

    public static final String INTERNAL_LOGIN_SUCCESS = "Đăng nhập nội bộ thành công";
    public static final String INTERNAL_LOGIN_NO_PERMISSION = "Tài khoản không có quyền đăng nhập nội bộ";
    public static final String CODE_INTERNAL_LOGIN_NO_PERMISSION = "AUTH_INTERNAL_NO_PERMISSION";
}
