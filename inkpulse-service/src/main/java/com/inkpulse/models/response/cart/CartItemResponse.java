package com.inkpulse.models.response.cart;

import java.math.BigDecimal;

public record CartItemResponse(
    String id,
    String editionId,
    String bookTitle,
    String authorName,
    String thumbnailUrl,
    BigDecimal price,
    String priceDisplay,
    int quantity,
    int stockQuantity,
    boolean stockSufficient
) {}
