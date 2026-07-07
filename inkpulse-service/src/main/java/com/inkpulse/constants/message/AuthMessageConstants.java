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

    public static final String FORGOT_PASSWORD_EMAIL_SENT = "Email hướng dẫn đặt lại mật khẩu đã được gửi";
    public static final String RESET_PASSWORD_SUCCESS = "Đặt lại mật khẩu thành công";
    public static final String INVALID_RESET_TOKEN = "Mã xác thực đặt lại mật khẩu không hợp lệ hoặc đã hết hạn";
    public static final String PASSWORD_CONFIRM_MISMATCH = "Mật khẩu xác nhận không trùng khớp";
    public static final String EMAIL_NOT_FOUND = "Email không tồn tại trong hệ thống";
    public static final String INVALID_GOOGLE_TOKEN = "Mã xác thực Google không hợp lệ";

    // Request Validation Messages
    public static final String EMAIL_NOT_BLANK = "Email không được để trống";
    public static final String EMAIL_INVALID = "Email không hợp lệ";
    public static final String DEVICE_ID_NOT_BLANK = "Device ID không được để trống";
    public static final String FINGERPRINT_NOT_BLANK = "Fingerprint không được để trống";
    
    public static final String TOKEN_NOT_BLANK = "Token không được để trống";
    public static final String PASSWORD_NEW_NOT_BLANK = "Mật khẩu mới không được để trống";
    public static final String PASSWORD_MIN_SIZE = "Mật khẩu phải có tối thiểu 6 ký tự";
    public static final String PASSWORD_CONFIRM_NOT_BLANK = "Xác nhận mật khẩu mới không được để trống";
    
    public static final String ID_TOKEN_NOT_BLANK = "ID Token không được để trống";
    public static final String DEVICE_NAME_NOT_BLANK = "Tên thiết bị không được để trống";
    public static final String DEVICE_TYPE_NOT_BLANK = "Loại thiết bị không được để trống";
    
    public static final String GOOGLE_USER_ID_NOT_BLANK = "Google User ID không được để trống";
    public static final String NAME_NOT_BLANK = "Họ tên không được để trống";
    public static final String USERNAME_NOT_BLANK = "Tên đăng nhập không được để trống";
    public static final String GENDER_NOT_BLANK = "Giới tính không được để trống";
    public static final String DOB_NOT_NULL = "Ngày sinh không được để trống";
    public static final String LANGUAGE_NOT_BLANK = "Ngôn ngữ không được để trống";
    
    public static final String RECIPIENT_PHONE_NOT_BLANK = "Số điện thoại người nhận không được để trống";
    public static final String PROVINCE_NOT_NULL = "Tỉnh/Thành không được để trống";
    public static final String DISTRICT_NOT_NULL = "Quận/Huyện không được để trống";
    public static final String WARD_NOT_BLANK = "Phường/Xã không được để trống";
    public static final String STREET_NOT_BLANK = "Địa chỉ chi tiết không được để trống";
    
    public static final String GOOGLE_ACCOUNT_ALREADY_LINKED = "Tài khoản Google này đã được liên kết với một tài khoản khác";
}
