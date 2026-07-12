package com.inkpulse.features.order.rules;

import com.inkpulse.entities.Order;
import com.inkpulse.features.order.commands.ReturnOrderCommand;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ReturnOrderContext {
    private final ReturnOrderCommand command;
    private Order order;
}
