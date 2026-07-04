package com.inkpulse.features.author.commands;

import com.inkpulse.cqrs.Command;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAuthorCommand implements Command<Boolean> {
    private UUID id;
}
