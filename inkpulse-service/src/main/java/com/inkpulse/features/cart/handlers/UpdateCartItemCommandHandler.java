package com.inkpulse.features.cart.handlers;

import com.inkpulse.cqrs.Command;
import com.inkpulse.features.cart.commands.UpdateCartItemCommand;
import com.inkpulse.entities.CartItem;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.repositories.CartItemRepository;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.constants.message.CartMessageConstants;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateCartItemCommandHandler implements Command.CommandHandler<UpdateCartItemCommand, Void> {

    private final CartItemRepository cartItemRepository;
    private final TokenService tokenService;

    @Override
    @Transactional
    public Void handle(UpdateCartItemCommand cmd) {
        // 1. Find CartItem
        CartItem cartItem = cartItemRepository.findById(cmd.getCartItemId())
                .orElseThrow(() -> new ResourceNotFoundException(CartMessageConstants.CART_ITEM_NOT_FOUND));

        // 2. Security validation
        if (cartItem.getCart().getUser() == null || !cartItem.getCart().getUser().getId().equals(cmd.getUserId())) {
            throw new UnauthorizedException("Bạn không có quyền chỉnh sửa giỏ hàng này");
        }

        // 3. Quantity validation
        if (cmd.getNewQuantity() <= 0) {
            throw new BusinessValidationException(CartMessageConstants.INVALID_QUANTITY);
        }

        // 4. Stock validation
        BookEdition edition = cartItem.getEdition();
        if (cmd.getNewQuantity() > edition.getStockQuantity()) {
            throw new BusinessValidationException(
                    String.format("Không đủ hàng trong kho. Còn lại: %d cuốn", edition.getStockQuantity()),
                    "STOCK_INSUFFICIENT"
            );
        }

        // 5. Update and save
        cartItem.setQuantity(cmd.getNewQuantity());
        cartItemRepository.save(cartItem);

        // 6. Invalidate cart cache (cache-aside)
        tokenService.removeUserCart(cmd.getUserId());

        log.info("User {} updated cart item {} to quantity {}", cmd.getUserId(), cmd.getCartItemId(), cmd.getNewQuantity());

        return null;
    }
}
