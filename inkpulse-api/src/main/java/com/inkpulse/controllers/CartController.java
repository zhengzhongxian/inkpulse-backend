package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.features.cart.queries.GetMyCartCountQuery;
import com.inkpulse.features.cart.queries.GetMyCartQuery;
import com.inkpulse.features.cart.commands.AddToCartCommand;
import com.inkpulse.features.cart.commands.UpdateCartItemCommand;
import com.inkpulse.features.cart.commands.RemoveCartItemCommand;
import com.inkpulse.models.response.cart.AddToCartResponse;
import com.inkpulse.models.response.cart.CartItemResponse;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.constants.message.CartMessageConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final Pipeline pipeline;

    @GetMapping("/count")
    public ResponseEntity<ResultRes<Integer>> getMyCartCount(
            @AuthenticationPrincipal String userIdStr) {
        log.info("Request to get cart count for authenticated user: {}", userIdStr);

        UUID userId = UUID.fromString(userIdStr);
        Integer count = pipeline.send(new GetMyCartCountQuery(userId));

        return ResponseEntity.ok(ResultRes.successResult(count, CartMessageConstants.GET_CART_COUNT_SUCCESS, 200));
    }

    @PostMapping("/items")
    public ResponseEntity<ResultRes<AddToCartResponse>> addToCart(
            @AuthenticationPrincipal String userIdStr,
            @RequestBody AddToCartCommand cmd) {
        log.info("Request to add item to cart for user: {}, edition: {}", userIdStr, cmd.getEditionId());
        cmd.setUserId(UUID.fromString(userIdStr));
        AddToCartResponse response = pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(response, CartMessageConstants.ADD_TO_CART_SUCCESS, 200));
    }

    @GetMapping
    public ResponseEntity<ResultRes<PagedList<CartItemResponse>>> getMyCart(
            @AuthenticationPrincipal String userIdStr,
            GetMyCartQuery query) {
        log.info("Request to get cart list for user: {}, page: {}, size: {}", userIdStr, query.getPageNumber(), query.getPageSize());
        query.setUserId(UUID.fromString(userIdStr));
        PagedList<CartItemResponse> cartList = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(cartList, CartMessageConstants.GET_MY_CART_SUCCESS, 200));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ResultRes<Void>> updateCartItemQuantity(
            @AuthenticationPrincipal String userIdStr,
            @PathVariable("itemId") UUID itemId,
            @RequestBody UpdateCartItemCommand cmd) {
        log.info("Request to update cart item {} for user {} to quantity {}", itemId, userIdStr, cmd.getNewQuantity());
        cmd.setUserId(UUID.fromString(userIdStr));
        cmd.setCartItemId(itemId);
        pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(null, CartMessageConstants.CART_ITEM_UPDATED, 200));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ResultRes<Void>> removeCartItem(
            @AuthenticationPrincipal String userIdStr,
            @PathVariable("itemId") UUID itemId) {
        log.info("Request to remove cart item {} for user {}", itemId, userIdStr);
        RemoveCartItemCommand cmd = new RemoveCartItemCommand(UUID.fromString(userIdStr), itemId);
        pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(null, CartMessageConstants.CART_ITEM_REMOVED, 200));
    }
}

