package com.inkpulse.features.order.rules;

import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.User;
import com.inkpulse.entities.UserAddress;
import com.inkpulse.features.order.commands.CreateOrderCommand;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
public class CreateOrderContext {
    private final CreateOrderCommand command;
    private User user;
    private UserAddress address;
    private Map<UUID, BookEdition> editions;
}
