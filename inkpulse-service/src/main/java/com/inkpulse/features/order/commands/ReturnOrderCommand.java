package com.inkpulse.features.order.commands;

import com.inkpulse.cqrs.Command;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnOrderCommand implements Command<Void> {
    private String orderCode;
    private String adminUserId;
}
