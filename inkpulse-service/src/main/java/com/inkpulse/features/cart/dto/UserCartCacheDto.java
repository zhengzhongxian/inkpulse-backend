package com.inkpulse.features.cart.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;
import java.util.List;

@CacheSection(KeyConstants.SECTION_CART_ITEMS)
public record UserCartCacheDto(
    String userId,
    List<CartItemDto> items
) implements Cacheable {
    @Override
    public String cacheId() {
        return userId;
    }

    public record CartItemDto(
        String id,
        String editionId,
        int quantity
    ) {}
}
