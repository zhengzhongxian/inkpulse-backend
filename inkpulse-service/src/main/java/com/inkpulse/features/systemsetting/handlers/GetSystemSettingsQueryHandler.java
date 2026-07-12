package com.inkpulse.features.systemsetting.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.systemsetting.SystemSettingResponse;
import com.inkpulse.features.systemsetting.queries.GetSystemSettingsQuery;
import com.inkpulse.repositories.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetSystemSettingsQueryHandler implements Query.QueryHandler<GetSystemSettingsQuery, List<SystemSettingResponse>> {

    private final SystemSettingRepository systemSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SystemSettingResponse> handle(GetSystemSettingsQuery query) {
        log.info("Handling GetSystemSettingsQuery to retrieve all system settings");
        return systemSettingRepository.findAll().stream()
                .map(setting -> SystemSettingResponse.builder()
                        .id(setting.getId())
                        .settingKey(setting.getSettingKey())
                        .settingValue(setting.getSettingValue())
                        .description(setting.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
