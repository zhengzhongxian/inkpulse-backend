package com.inkpulse.constants.message;

public final class UserMessageConstants {
    private UserMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String USER_NOT_FOUND = "Không tìm thấy người dùng";
    public static final String PROFILE_UPDATE_SUCCESS = "Cập nhật thông tin cá nhân thành công!";
    public static final String AVATAR_UPLOAD_FAILED = "Không thể tải ảnh đại diện lên hệ thống: ";
    public static final String AVATAR_READ_ERROR = "Không thể đọc tệp ảnh đại diện";
    
    // Change Password
    public static final String OLD_PASSWORD_INCORRECT = "Mật khẩu hiện tại không chính xác";
    public static final String PASSWORD_MATCHES_CURRENT = "Mật khẩu mới không được trùng với mật khẩu hiện tại";
    public static final String PASSWORD_IN_HISTORY = "Mật khẩu mới không được trùng với 5 mật khẩu gần nhất";
    public static final String CHANGE_PASSWORD_SUCCESS = "Đổi mật khẩu thành công!";
    
    // MFA types
    public static final String MFA_TYPE_NOT_FOUND = "Không tìm thấy kiểu MFA: ";

    // Address
    public static final String ADDRESS_NOT_FOUND = "Địa chỉ không tồn tại";
    public static final String ACCESS_DENIED = "Bạn không có quyền thực hiện hành động này";
    public static final String CREATE_ADDRESS_SUCCESS = "Thêm địa chỉ thành công!";
    public static final String UPDATE_ADDRESS_SUCCESS = "Cập nhật địa chỉ thành công!";
    public static final String DELETE_ADDRESS_SUCCESS = "Xóa địa chỉ thành công!";
}
