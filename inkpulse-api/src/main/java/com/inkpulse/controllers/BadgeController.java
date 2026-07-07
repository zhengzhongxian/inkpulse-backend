package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.BadgeMessageConstants;
import com.inkpulse.features.badge.commands.CreateBadgeCommand;
import com.inkpulse.features.badge.commands.UpdateBadgeCommand;
import com.inkpulse.features.badge.commands.DeleteBadgeCommand;
import com.inkpulse.models.response.badge.BadgeResponse;
import com.inkpulse.features.badge.queries.GetBadgeByIdQuery;
import com.inkpulse.features.badge.queries.GetBadgesQuery;
import com.inkpulse.models.response.ResultRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BadgeController {

    private final Pipeline pipeline;

    @GetMapping("/api/v1/badges")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Badges.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<List<BadgeResponse>>> getBadges() {
        log.info("REST request to get badges list");
        List<BadgeResponse> result = pipeline.send(new GetBadgesQuery());
        return ResponseEntity.ok(ResultRes.successResult(result, BadgeMessageConstants.LIST_SUCCESS, 200));
    }

    @PostMapping("/api/v1/badges")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Badges.CREATE + "')")
    public ResponseEntity<ResultRes<BadgeResponse>> createBadge(@RequestBody CreateBadgeCommand command) {
        log.info("REST request to create badge: {}", command.getText());
        BadgeResponse result = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(result, BadgeMessageConstants.CREATE_SUCCESS, 200));
    }

    @GetMapping("/api/v1/badges/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Badges.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<BadgeResponse>> getBadgeById(@PathVariable("id") UUID id) {
        log.info("REST request to get badge details: {}", id);
        BadgeResponse result = pipeline.send(new GetBadgeByIdQuery(id));
        return ResponseEntity.ok(ResultRes.successResult(result, BadgeMessageConstants.GET_SUCCESS, 200));
    }

    @PutMapping("/api/v1/badges/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Badges.EDIT + "')")
    public ResponseEntity<ResultRes<BadgeResponse>> updateBadge(
            @PathVariable("id") UUID id,
            @RequestBody UpdateBadgeCommand command) {
        log.info("REST request to update badge: {}", id);
        command.setId(id);
        BadgeResponse result = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(result, BadgeMessageConstants.UPDATE_SUCCESS, 200));
    }

    @DeleteMapping("/api/v1/badges/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Badges.DELETE + "')")
    public ResponseEntity<ResultRes<Boolean>> deleteBadge(@PathVariable("id") UUID id) {
        log.info("REST request to delete badge: {}", id);
        Boolean result = pipeline.send(new DeleteBadgeCommand(id));
        return ResponseEntity.ok(ResultRes.successResult(result, BadgeMessageConstants.DELETE_SUCCESS, 200));
    }
}
