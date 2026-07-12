package com.inkpulse.features.systemsetting.commands;

import com.inkpulse.cqrs.Command;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSystemSettingCommand implements Command<Void> {
    private UUID id;
    private String settingValue;
}
