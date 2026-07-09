package com.inkpulse.features.order.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.order.ConfirmPackOrderResponse;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPackOrderCommand implements Command<ConfirmPackOrderResponse> {
    private UUID adminUserId;
    private String orderCode;
}
