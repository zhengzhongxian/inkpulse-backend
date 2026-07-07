package com.inkpulse.features.cart.commands;

import com.inkpulse.cqrs.Command;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoveCartItemCommand implements Command<Void> {
    private UUID userId;
    private UUID cartItemId;
}
