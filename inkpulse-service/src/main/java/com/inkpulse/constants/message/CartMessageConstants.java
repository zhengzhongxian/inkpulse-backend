package com.inkpulse.constants.message;

public final class CartMessageConstants {

    private CartMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String GET_CART_COUNT_SUCCESS = "Lấy số lượng giỏ hàng thành công!";
    public static final String ADD_TO_CART_SUCCESS = "Thêm vào giỏ hàng thành công!";
    public static final String CART_ITEM_UPDATED = "Cập nhật số lượng thành công!";
    public static final String CART_ITEM_REMOVED = "Xóa khỏi giỏ hàng thành công!";
    public static final String STOCK_INSUFFICIENT = "Số lượng tồn kho không đủ!";
    public static final String EDITION_NOT_FOUND = "Không tìm thấy phiên bản sách này!";
    public static final String GET_MY_CART_SUCCESS = "Lấy danh sách giỏ hàng thành công!";
    public static final String CART_ITEM_NOT_FOUND = "Không tìm thấy sản phẩm trong giỏ hàng!";
    public static final String INVALID_QUANTITY = "Số lượng không hợp lệ!";
}

