package com.inkpulse.constants.message;

public final class AddressMessageConstants {

    private AddressMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String GET_PROVINCES_SUCCESS = "Lấy danh sách tỉnh thành công!";
    public static final String GET_DISTRICTS_SUCCESS = "Lấy danh sách quận/huyện thành công!";
    public static final String GET_WARDS_SUCCESS = "Lấy danh sách phường/xã thành công!";

    public static final String PROVINCE_NOT_FOUND = "Tỉnh/Thành phố không tồn tại";
    public static final String DISTRICT_NOT_FOUND = "Quận/Huyện không tồn tại";
    public static final String WARD_NOT_FOUND = "Phường/Xã không tồn tại";

    public static final String INVALID_DISTRICT_PROVINCE = "Quận/Huyện không thuộc Tỉnh/Thành phố đã chọn";
    public static final String INVALID_WARD_DISTRICT = "Phường/Xã không thuộc Quận/Huyện đã chọn";
}
