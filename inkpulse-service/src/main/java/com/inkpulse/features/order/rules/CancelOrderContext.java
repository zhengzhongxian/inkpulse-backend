package com.inkpulse.features.order.rules;

import com.inkpulse.entities.Order;
import com.inkpulse.features.order.commands.CancelOrderCommand;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class CancelOrderContext {
    private final CancelOrderCommand command;
    private Order order;
}
