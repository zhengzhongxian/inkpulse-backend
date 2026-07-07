package com.inkpulse.features.cart.handlers;

import com.inkpulse.cqrs.Command;
import com.inkpulse.features.cart.commands.RemoveCartItemCommand;
import com.inkpulse.entities.CartItem;
import com.inkpulse.repositories.CartItemRepository;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.constants.message.CartMessageConstants;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import com.inkpulse.corehelpers.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveCartItemCommandHandler implements Command.CommandHandler<RemoveCartItemCommand, Void> {

    private final CartItemRepository cartItemRepository;
    private final TokenService tokenService;

    @Override
    @Transactional
    public Void handle(RemoveCartItemCommand cmd) {
        // 1. Find CartItem
        CartItem cartItem = cartItemRepository.findById(cmd.getCartItemId())
                .orElseThrow(() -> new ResourceNotFoundException(CartMessageConstants.CART_ITEM_NOT_FOUND));

        // 2. Security validation
        if (cartItem.getCart().getUser() == null || !cartItem.getCart().getUser().getId().equals(cmd.getUserId())) {
            throw new UnauthorizedException("Bạn không có quyền chỉnh sửa giỏ hàng này");
        }

        // 3. Hard delete CartItem
        cartItemRepository.delete(cartItem);

        // 4. Invalidate cart cache (cache-aside)
        tokenService.removeUserCart(cmd.getUserId());

        log.info("User {} removed cart item {} from cart", cmd.getUserId(), cmd.getCartItemId());

        return null;
    }
}
