package com.inkpulse.features.order.commands;

import com.inkpulse.cqrs.Command;
import java.util.UUID;

public record ApproveOrderCommand(UUID adminUserId, String orderCode) implements Command<Void> {
}
