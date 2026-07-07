package com.inkpulse.features.cart.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.features.cart.queries.GetMyCartCountQuery;
import com.inkpulse.features.cart.dto.UserCartCacheDto;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.cache.SectionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetMyCartCountQueryHandler implements Query.QueryHandler<GetMyCartCountQuery, Integer> {

    private final SectionCacheService sectionCache;
    private final TokenService tokenService;

    @Override
    @Transactional(readOnly = true)
    public Integer handle(GetMyCartCountQuery query) {
        String userIdStr = query.userId().toString();
        UserCartCacheDto cart = sectionCache.get(userIdStr, UserCartCacheDto.class);
        if (cart == null) {
            tokenService.cacheUserCart(query.userId());
            cart = sectionCache.get(userIdStr, UserCartCacheDto.class);
        }
        if (cart == null || cart.items() == null) {
            return 0;
        }
        return cart.items().stream()
                .mapToInt(UserCartCacheDto.CartItemDto::quantity)
                .sum();
    }
}
