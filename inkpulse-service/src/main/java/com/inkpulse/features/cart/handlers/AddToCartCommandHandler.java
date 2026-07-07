package com.inkpulse.features.cart.handlers;

import com.inkpulse.cqrs.Command;
import com.inkpulse.features.cart.commands.AddToCartCommand;
import com.inkpulse.models.response.cart.AddToCartResponse;
import com.inkpulse.entities.Cart;
import com.inkpulse.entities.CartItem;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.User;
import com.inkpulse.repositories.CartRepository;
import com.inkpulse.repositories.CartItemRepository;
import com.inkpulse.repositories.BookEditionRepository;
import com.inkpulse.repositories.UserRepository;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.constants.message.CartMessageConstants;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddToCartCommandHandler implements Command.CommandHandler<AddToCartCommand, AddToCartResponse> {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookEditionRepository bookEditionRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Override
    @Transactional
    public AddToCartResponse handle(AddToCartCommand cmd) {
        // 1. Retrieve book edition
        BookEdition edition = bookEditionRepository.findById(cmd.getEditionId())
                .orElseThrow(() -> new ResourceNotFoundException(CartMessageConstants.EDITION_NOT_FOUND));

        int qtyToAdd = cmd.getQuantity() <= 0 ? 1 : cmd.getQuantity();

        // 2. Retrieve or create Cart
        Cart cart = cartRepository.findByUserId(cmd.getUserId()).orElse(null);
        if (cart == null) {
            User user = userRepository.findById(cmd.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            cart = Cart.builder()
                    .user(user)
                    .build();
            cart = cartRepository.save(cart);
        }

        // 3. Find existing CartItem or create new one
        CartItem cartItem = cartItemRepository.findByCartIdAndEditionId(cart.getId(), edition.getId()).orElse(null);
        int newQty;
        if (cartItem != null) {
            newQty = cartItem.getQuantity() + qtyToAdd;
            if (newQty > edition.getStockQuantity()) {
                throw new BusinessValidationException(
                        String.format("Không đủ hàng trong kho. Còn lại: %d cuốn, giỏ hàng của bạn đang có: %d cuốn", 
                                edition.getStockQuantity(), cartItem.getQuantity()),
                        "STOCK_INSUFFICIENT"
                );
            }
            cartItem.setQuantity(newQty);
            cartItemRepository.save(cartItem);
        } else {
            newQty = qtyToAdd;
            if (newQty > edition.getStockQuantity()) {
                throw new BusinessValidationException(
                        String.format("Không đủ hàng trong kho. Còn lại: %d cuốn", edition.getStockQuantity()),
                        "STOCK_INSUFFICIENT"
                );
            }
            // Do not manually assign ID before save!
            cartItem = CartItem.builder()
                    .cart(cart)
                    .edition(edition)
                    .quantity(newQty)
                    .build();
            cartItem = cartItemRepository.save(cartItem);
        }

        // 4. Invalidate cache (cache-aside)
        tokenService.removeUserCart(cmd.getUserId());

        // 5. Count total quantity in cart
        List<CartItem> allItems = cartItemRepository.findByCartId(cart.getId());
        int cartTotalItems = allItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        log.info("User {} added {} to cart. Total items: {}", cmd.getUserId(), edition.getId(), cartTotalItems);

        return new AddToCartResponse(
                cartItem.getId().toString(),
                newQty,
                cartTotalItems,
                CartMessageConstants.ADD_TO_CART_SUCCESS
        );
    }
}
