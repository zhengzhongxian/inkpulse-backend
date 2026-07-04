package com.inkpulse.constants.message;

public final class CategoryMessageConstants {

    private CategoryMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String GET_CATEGORIES_SUCCESS = "Lấy danh mục thành công!";
    public static final String CREATE_SUCCESS = "Tạo danh mục mới thành công!";
    public static final String UPDATE_SUCCESS = "Cập nhật danh mục thành công!";
    public static final String DELETE_SUCCESS = "Xóa danh mục thành công!";
    public static final String LIST_SUCCESS = "Lấy danh sách danh mục thành công!";
    public static final String CATEGORY_NOT_FOUND = "Không tìm thấy danh mục";
    public static final String CODE_CATEGORY_NOT_FOUND = "CATEGORY_NOT_FOUND";
    public static final String SLUG_ALREADY_EXISTS = "Slug danh mục đã tồn tại";
    public static final String CODE_SLUG_ALREADY_EXISTS = "CATEGORY_SLUG_ALREADY_EXISTS";
}
