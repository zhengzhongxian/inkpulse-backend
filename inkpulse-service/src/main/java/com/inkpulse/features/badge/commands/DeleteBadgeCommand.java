package com.inkpulse.features.badge.commands;

import com.inkpulse.cqrs.Command;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteBadgeCommand implements Command<Boolean> {
    private UUID id;
}
