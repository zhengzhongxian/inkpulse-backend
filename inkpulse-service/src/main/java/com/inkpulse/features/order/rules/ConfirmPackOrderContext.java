package com.inkpulse.features.order.rules;

import com.inkpulse.entities.Order;
import com.inkpulse.features.order.commands.ConfirmPackOrderCommand;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ConfirmPackOrderContext {
    private final ConfirmPackOrderCommand command;
    private Order order;
}
