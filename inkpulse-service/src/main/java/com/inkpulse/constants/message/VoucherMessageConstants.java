package com.inkpulse.constants.message;

public final class VoucherMessageConstants {

    private VoucherMessageConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String CREATE_SUCCESS = "Tạo mã giảm giá mới thành công!";
    public static final String UPDATE_SUCCESS = "Cập nhật mã giảm giá thành công!";
    public static final String DELETE_SUCCESS = "Xóa mã giảm giá thành công!";
    public static final String GET_LIST_SUCCESS = "Lấy danh sách mã giảm giá thành công!";
    public static final String GET_DETAIL_SUCCESS = "Lấy chi tiết mã giảm giá thành công!";

    public static final String VOUCHER_NOT_FOUND = "Không tìm thấy mã giảm giá!";
    public static final String CODE_ALREADY_EXISTS = "Mã giảm giá này đã tồn tại trên hệ thống!";
    public static final String START_DATE_AFTER_END_DATE = "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!";
    public static final String START_DATE_PAST = "Ngày bắt đầu không được ở quá khứ!";
    public static final String END_DATE_PAST = "Ngày kết thúc không được ở quá khứ!";
    public static final String DISCOUNT_VALUE_INVALID = "Giá trị giảm giá phải lớn hơn 0!";
    public static final String DISCOUNT_PERCENTAGE_EXCEEDED = "Giá trị giảm giá theo phần trăm không được vượt quá 100%!";
    public static final String MIN_ORDER_VALUE_INVALID = "Giá trị đơn hàng tối thiểu phải lớn hơn hoặc bằng 0!";
    public static final String MAX_USES_INVALID = "Số lượt sử dụng tối đa phải lớn hơn 0!";
    public static final String MAX_USES_PER_USER_INVALID = "Số lượt sử dụng tối đa của mỗi người dùng phải lớn hơn 0!";
    public static final String COIN_COST_INVALID = "Số xu quy đổi phải lớn hơn hoặc bằng 0!";
    
    public static final String TARGET_IDS_REQUIRED = "Vui lòng chọn danh mục, sách hoặc phiên bản áp dụng!";
    public static final String TARGET_IDS_NOT_FOUND = "Một số mục áp dụng không tồn tại trên hệ thống!";
    
    public static final String VOUCHER_ALREADY_USED_CANNOT_MODIFY = "Mã giảm giá đã có người sử dụng, không được phép chỉnh sửa loại giảm giá, giá trị giảm giá, đối tượng áp dụng hoặc chi phí xu!";
    public static final String EXCHANGE_SUCCESS = "Đổi mã giảm giá thành công!";
    public static final String INSUFFICIENT_COINS = "Bạn không đủ xu để đổi mã giảm giá này!";
    public static final String VOUCHER_EXPIRED = "Mã giảm giá này đã hết hạn hoặc ngưng hoạt động!";
    public static final String VOUCHER_OUT_OF_STOCK = "Mã giảm giá này đã hết lượt sử dụng!";
    public static final String LIMIT_PER_USER_EXCEEDED = "Bạn đã đạt giới hạn đổi mã giảm giá này!";
    public static final String VOUCHER_NOT_OWNED_OR_USED = "Bạn không sở hữu mã giảm giá này hoặc mã đã được sử dụng!";
    public static final String VOUCHER_MIN_ORDER_VALUE_NOT_MET = "Giá trị các sản phẩm được áp dụng trong đơn hàng không đạt mức tối thiểu!";
    public static final String VOUCHER_SHIPPING_MIN_ORDER_VALUE_NOT_MET = "Giá trị đơn hàng không đạt mức tối thiểu để áp dụng mã giảm giá vận chuyển!";
    public static final String VOUCHER_NO_ELIGIBLE_ITEMS = "Không có sản phẩm nào trong đơn hàng được áp dụng mã giảm giá này!";
    public static final String LOGIN_REQUIRED = "Vui lòng đăng nhập để thực hiện chức năng này!";

    // Error Codes
    public static final String CODE_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String CODE_VOUCHER_NOT_FOUND = "VOUCHER_NOT_FOUND";
    public static final String CODE_CODE_ALREADY_EXISTS = "VOUCHER_CODE_ALREADY_EXISTS";
    public static final String CODE_INVALID_DATE = "VOUCHER_INVALID_DATE";
    public static final String CODE_INVALID_DISCOUNT = "VOUCHER_INVALID_DISCOUNT";
    public static final String CODE_TARGET_IDS_REQUIRED = "VOUCHER_TARGET_IDS_REQUIRED";
    public static final String CODE_TARGET_IDS_NOT_FOUND = "VOUCHER_TARGET_IDS_NOT_FOUND";
    public static final String CODE_VOUCHER_USED_LOCKED = "VOUCHER_USED_LOCKED";
    public static final String CODE_INVALID_FIELDS = "VOUCHER_INVALID_FIELDS";
    public static final String CODE_INSUFFICIENT_COINS = "INSUFFICIENT_COINS";
    public static final String CODE_VOUCHER_EXPIRED = "VOUCHER_EXPIRED";
    public static final String CODE_VOUCHER_OUT_OF_STOCK = "VOUCHER_OUT_OF_STOCK";
    public static final String CODE_LIMIT_PER_USER_EXCEEDED = "LIMIT_PER_USER_EXCEEDED";
    public static final String CODE_VOUCHER_NOT_OWNED_OR_USED = "VOUCHER_NOT_OWNED_OR_USED";
    public static final String CODE_VOUCHER_MIN_ORDER_VALUE_NOT_MET = "VOUCHER_MIN_ORDER_VALUE_NOT_MET";
    public static final String CODE_VOUCHER_SHIPPING_MIN_ORDER_VALUE_NOT_MET = "VOUCHER_SHIPPING_MIN_ORDER_VALUE_NOT_MET";
    public static final String CODE_VOUCHER_NO_ELIGIBLE_ITEMS = "VOUCHER_NO_ELIGIBLE_ITEMS";
}
