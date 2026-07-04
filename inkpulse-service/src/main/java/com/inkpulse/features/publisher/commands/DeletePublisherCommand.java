package com.inkpulse.features.publisher.commands;

import com.inkpulse.cqrs.Command;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeletePublisherCommand implements Command<Boolean> {
    private UUID id;
}
