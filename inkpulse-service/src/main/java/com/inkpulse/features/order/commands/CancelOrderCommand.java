package com.inkpulse.features.order.commands;

import com.inkpulse.cqrs.Command;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderCommand implements Command<Void> {
    private String orderCode;
    private String userId; // Caller's user ID (could be customer or admin)
    private boolean isAdmin; // Flag to indicate if the caller has admin rights
}
