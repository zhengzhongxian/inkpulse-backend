package com.inkpulse.constants.message;

public final class PublisherMessageConstants {
    private PublisherMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String PUBLISHER_NOT_FOUND = "Không tìm thấy nhà xuất bản";
    public static final String CODE_PUBLISHER_NOT_FOUND = "PUBLISHER_001";

    public static final String PUBLISHER_NAME_EMPTY = "Tên nhà xuất bản không được để trống";
    public static final String CODE_PUBLISHER_NAME_EMPTY = "PUBLISHER_002";

    public static final String LIST_SUCCESS = "Lấy danh sách nhà xuất bản thành công";
    public static final String GET_SUCCESS = "Lấy thông tin nhà xuất bản thành công";
    public static final String CREATE_SUCCESS = "Thêm nhà xuất bản thành công";
    public static final String UPDATE_SUCCESS = "Cập nhật nhà xuất bản thành công";
    public static final String DELETE_SUCCESS = "Xóa nhà xuất bản thành công";
}
