package com.inkpulse.constants.message;

public final class OrderMessageConstants {

    private OrderMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String CREATE_ORDER_SUCCESS = "Đặt hàng thành công!";
    public static final String CALCULATE_FEE_SUCCESS = "Tính phí vận chuyển thành công!";
    public static final String STOCK_INSUFFICIENT = "Sản phẩm '%s' không đủ số lượng tồn kho (còn %d)!";
    public static final String INVALID_ITEMS = "Danh sách sản phẩm không được trống!";
    public static final String INVALID_QUANTITY = "Số lượng sản phẩm phải lớn hơn 0!";
    public static final String MAX_ITEMS_EXCEEDED = "Tối đa 20 sản phẩm trong một đơn hàng!";
    public static final String INVALID_ADDRESS = "Địa chỉ giao hàng không hợp lệ!";
    public static final String ADDRESS_NOT_FOUND = "Không tìm thấy địa chỉ giao hàng!";
    public static final String INVALID_PAYMENT_METHOD = "Phương thức thanh toán không hợp lệ!";
    public static final String ORDER_NOT_FOUND = "Không tìm thấy đơn hàng!";
    public static final String EDITION_NOT_FOUND = "Không tìm thấy phiên bản sách!";
    public static final String SHIPPING_FEE_CALCULATION_FAILED = "Tính phí vận chuyển thất bại!";
    public static final String GHN_CREATE_ORDER_FAILED = "Tạo đơn vận chuyển GHN thất bại!";
    public static final String PAYOS_CREATE_LINK_FAILED = "Tạo link thanh toán PayOS thất bại!";
    public static final String WEBHOOK_PROCESSED = "Webhook đã được xử lý!";
    public static final String WEBHOOK_INVALID = "Dữ liệu webhook không hợp lệ!";
    public static final String WEBHOOK_TEST = "PayOS test webhook - bỏ qua!";
    public static final String GET_ORDERS_SUCCESS = "Lấy danh sách đơn hàng thành công!";
    public static final String GET_ORDER_DETAIL_SUCCESS = "Lấy chi tiết đơn hàng thành công!";
    public static final String CONFIRM_PACK_SUCCESS = "Xác nhận đóng gói đơn hàng thành công!";
    public static final String ORDER_NOT_PROCESSING = "Đơn hàng không ở trạng thái đang xử lý!";
    public static final String ORDER_NOT_PAID = "Đơn hàng chưa được thanh toán!";
    public static final String ORDER_ALREADY_PACKED = "Đơn hàng đã được đóng gói trước đó!";
    public static final String GHN_WEBHOOK_PROCESSED = "GHN Webhook đã được xử lý!";
    public static final String GHN_WEBHOOK_INVALID = "Dữ liệu GHN Webhook không hợp lệ!";
    public static final String GET_ORDER_LOGS_SUCCESS = "Lấy lịch sử trạng thái đơn hàng thành công!";
}
