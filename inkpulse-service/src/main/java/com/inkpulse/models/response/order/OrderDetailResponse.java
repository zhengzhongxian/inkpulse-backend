package com.inkpulse.models.response.order;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;
import java.util.List;

@CacheSection(KeyConstants.SECTION_ORDER_DETAIL)
public record OrderDetailResponse(
    String orderId,
    String userId,
    String orderCode,
    String ghnOrderCode,
    String orderStatus,
    String paymentMethod,
    String paymentStatus,
    String receiverName,
    String recipientPhone,
    String shippingAddress,
    String addressLabel,
    String orderFeeDisplay,
    String shippingFeeDisplay,
    String totalDisplay,
    List<OrderItemDetailResponse> items,
    String createdAt,
    String voucherCode,
    String voucherDiscountAmountDisplay
) implements Cacheable {
    @Override
    public String cacheId() {
        return orderId;
    }
}
