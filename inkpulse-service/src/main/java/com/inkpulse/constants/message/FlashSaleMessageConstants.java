package com.inkpulse.constants.message;

public final class FlashSaleMessageConstants {

    private FlashSaleMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String CREATE_SUCCESS = "Tạo chương trình Flash Sale thành công!";
    public static final String UPDATE_SUCCESS = "Cập nhật Flash Sale thành công!";
    public static final String DELETE_SUCCESS = "Xóa Flash Sale thành công!";
    public static final String GET_LIST_SUCCESS = "Lấy danh sách Flash Sale thành công!";
    public static final String GET_DETAIL_SUCCESS = "Lấy chi tiết Flash Sale thành công!";
    
    public static final String FLASHSALE_NOT_FOUND = "Không tìm thấy chương trình Flash Sale!";
    public static final String OUT_OF_STOCK = "Sản phẩm Flash Sale này đã hết hàng!";
    public static final String ALREADY_PURCHASED = "Bạn đã tham gia mua sản phẩm Flash Sale này rồi!";
    public static final String NOT_IN_PERIOD = "Chương trình Flash Sale này chưa bắt đầu hoặc đã kết thúc!";
    public static final String EDITION_NOT_FOUND = "Không tìm thấy phiên bản sách này!";

    public static final String CODE_FLASHSALE_NOT_FOUND = "FLASHSALE_NOT_FOUND";
    public static final String CODE_OUT_OF_STOCK = "OUT_OF_STOCK";
    public static final String CODE_ALREADY_PURCHASED = "ALREADY_PURCHASED";
    public static final String CODE_NOT_IN_PERIOD = "NOT_IN_PERIOD";
    public static final String CODE_EDITION_NOT_FOUND = "EDITION_NOT_FOUND";
}
