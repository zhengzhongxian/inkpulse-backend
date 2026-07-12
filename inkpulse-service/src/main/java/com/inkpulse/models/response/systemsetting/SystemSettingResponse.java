package com.inkpulse.models.response.systemsetting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingResponse {
    private UUID id;
    private String settingKey;
    private String settingValue;
    private String description;
}
