package com.inkpulse.controllers;

import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.SystemSettingMessageConstants;
import com.inkpulse.features.systemsetting.commands.UpdateSystemSettingCommand;
import com.inkpulse.models.response.systemsetting.SystemSettingResponse;
import com.inkpulse.models.request.systemsetting.UpdateSystemSettingRequest;
import com.inkpulse.features.systemsetting.queries.GetSystemSettingsQuery;
import com.inkpulse.models.response.ResultRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import an.awesome.pipelinr.Pipeline;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/system-settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final Pipeline pipeline;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.SystemSettings.VIEW + "')")
    public ResponseEntity<ResultRes<List<SystemSettingResponse>>> getSystemSettings() {
        log.info("Request to get all system settings");
        List<SystemSettingResponse> response = pipeline.send(new GetSystemSettingsQuery());
        return ResponseEntity.ok(ResultRes.successResult(response, SystemSettingMessageConstants.GET_SUCCESS, 200));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.SystemSettings.UPDATE + "')")
    public ResponseEntity<ResultRes<Void>> updateSystemSetting(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateSystemSettingRequest request) {
        log.info("Request to update system setting ID: {} with value: {}", id, request.getSettingValue());
        
        UpdateSystemSettingCommand command = UpdateSystemSettingCommand.builder()
                .id(id)
                .settingValue(request.getSettingValue())
                .build();
        pipeline.send(command);
        
        return ResponseEntity.ok(ResultRes.successResult(null, SystemSettingMessageConstants.UPDATE_SUCCESS, 200));
    }
}
