package com.inkpulse.models.response.cart;

public record AddToCartResponse(
    String cartItemId,
    int newQuantity,
    int cartTotalItems,
    String message
) {}
