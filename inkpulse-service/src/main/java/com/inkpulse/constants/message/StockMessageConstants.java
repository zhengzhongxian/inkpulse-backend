package com.inkpulse.constants.message;

public final class StockMessageConstants {

    private StockMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String IMPORT_SUCCESS = "Nhập kho thành công!";
    public static final String ADJUST_SUCCESS = "Điều chỉnh tồn kho thành công!";
    public static final String STOCK_INSUFFICIENT = "Sản phẩm '%s' chỉ còn %d trong kho.";
    public static final String EDITION_NOT_FOUND = "Không tìm thấy phiên bản sách.";
    public static final String INVALID_QUANTITY = "Số lượng không hợp lệ.";
    public static final String GET_HISTORY_SUCCESS = "Lấy lịch sử kho thành công!";
    public static final String STOCK_ADJUST_INSUFFICIENT = "Tồn kho hiện tại không đủ để giảm xuống %d.";

    public static final String CODE_EDITION_NOT_FOUND = "EDITION_NOT_FOUND";
    public static final String CODE_STOCK_INSUFFICIENT = "STOCK_INSUFFICIENT";
    public static final String CODE_INVALID_QUANTITY = "INVALID_QUANTITY";
    public static final String CODE_STOCK_ADJUST_FAILED = "STOCK_ADJUST_FAILED";
}
