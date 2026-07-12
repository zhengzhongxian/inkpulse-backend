package com.inkpulse.features.systemsetting.handlers;

import com.inkpulse.constants.SystemSettingConstant;
import com.inkpulse.constants.message.SystemSettingMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.SystemSetting;
import com.inkpulse.features.systemsetting.commands.UpdateSystemSettingCommand;
import com.inkpulse.repositories.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateSystemSettingHandler implements Command.CommandHandler<UpdateSystemSettingCommand, Void> {

    private final SystemSettingRepository systemSettingRepository;

    @Override
    @Transactional
    public Void handle(UpdateSystemSettingCommand command) {
        log.info("Handling UpdateSystemSettingCommand for ID: {}", command.getId());

        // 1. Find by ID
        Optional<SystemSetting> settingOpt = systemSettingRepository.findById(command.getId());
        if (settingOpt.isEmpty()) {
            throw new BusinessValidationException(SystemSettingMessageConstants.ID_NOT_FOUND, "SETTING_NOT_FOUND");
        }

        SystemSetting setting = settingOpt.get();

        // 2. Validate value based on the setting key
        String trimmedValue = command.getSettingValue() != null ? command.getSettingValue().trim() : "";
        if (SystemSettingConstant.BonusCoins.KEY.equals(setting.getSettingKey())) {
            try {
                int bonusCoins = Integer.parseInt(trimmedValue);
                if (bonusCoins < 0) {
                    throw new BusinessValidationException(SystemSettingMessageConstants.BONUS_COINS_INVALID, "INVALID_BONUS_COINS");
                }
            } catch (NumberFormatException e) {
                throw new BusinessValidationException(SystemSettingMessageConstants.BONUS_COINS_INVALID, "INVALID_BONUS_COINS");
            }
        }

        // 3. Update value
        setting.setSettingValue(trimmedValue);
        systemSettingRepository.save(setting);

        log.info("Successfully updated system setting. Key: {}, New Value: {}", setting.getSettingKey(), trimmedValue);
        return null;
    }
}
