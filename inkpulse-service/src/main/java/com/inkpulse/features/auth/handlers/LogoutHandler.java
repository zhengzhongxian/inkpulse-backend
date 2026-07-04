package com.inkpulse.features.auth.handlers;

import com.inkpulse.cqrs.Command;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.features.auth.commands.LogoutCommand;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutHandler implements Command.CommandHandler<LogoutCommand, Void> {

    private final TokenService tokenService;

    @Override
    public Void handle(LogoutCommand cmd) {
        // 1. Revoke the specific refresh token used for this session
        try {
            tokenService.revokeRefreshToken(cmd.getRefreshToken());
        } catch (Exception e) {
            log.debug("Refresh token already invalid during logout: {}", e.getMessage());
        }

        // 2. Remove user session and cart cache from Redis
        try {
            UUID userId = tokenService.getUserIdFromToken(cmd.getAccessToken());
            tokenService.removeUserSession(userId);
            tokenService.removeUserCart(userId);
        } catch (Exception e) {
            log.debug("Access token already invalid during logout: {}", e.getMessage());
        }

        // 3. Blacklist the access token by its jti until natural expiry
        try {
            Claims claims = tokenService.parseAccessToken(cmd.getAccessToken());
            String jti = claims.getId();
            String userId = claims.getSubject();
            if (jti != null && userId != null) {
                tokenService.blacklistAccessToken(jti, userId, cmd.getReasonCode());
            }
        } catch (Exception e) {
            log.debug("Could not blacklist access token (already expired?): {}", e.getMessage());
        }

        return null;
    }
}
