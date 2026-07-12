package com.inkpulse.models.response.order;

public record OrderItemDetailResponse(
    String editionId,
    String bookTitle,
    String authorName,
    String thumbnailUrl,
    int quantity,
    String priceDisplay,
    String subtotalDisplay,
    Integer editionNumber,
    String coverType,
    String isbn
) {}
