package com.inkpulse.features.cart.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.cart.AddToCartResponse;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartCommand implements Command<AddToCartResponse> {
    private UUID userId;
    private UUID editionId;
    private int quantity;
}
