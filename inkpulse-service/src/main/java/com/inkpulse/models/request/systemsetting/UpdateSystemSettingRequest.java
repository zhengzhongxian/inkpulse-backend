package com.inkpulse.models.request.systemsetting;

import com.inkpulse.constants.message.SystemSettingMessageConstants;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSystemSettingRequest {

    @NotBlank(message = SystemSettingMessageConstants.VALUE_NOT_BLANK)
    private String settingValue;
}
