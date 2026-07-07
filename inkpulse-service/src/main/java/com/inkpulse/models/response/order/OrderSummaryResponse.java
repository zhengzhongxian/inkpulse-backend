package com.inkpulse.models.response.order;

public record OrderSummaryResponse(
    String orderId,
    String orderCode,
    String orderStatus,
    String paymentMethod,
    String paymentStatus,
    String orderFeeDisplay,
    String shippingFeeDisplay,
    String totalDisplay,
    int totalItems,
    String firstItemTitle,
    String firstItemThumbnail,
    String createdAt
) {}
