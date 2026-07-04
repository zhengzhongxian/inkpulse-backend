package com.inkpulse.features.auth.handlers;

import com.inkpulse.cqrs.Command;
import com.inkpulse.constants.message.TokenMessageConstants;
import com.inkpulse.corehelpers.exceptions.TokenRefreshException;
import com.inkpulse.entities.User;
import com.inkpulse.features.auth.commands.RefreshTokenCommand;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.features.auth.service.TokenService.RotationResult;
import com.inkpulse.features.auth.service.TokenService.RotationStatus;
import com.inkpulse.models.response.LoginResult;
import com.inkpulse.repositories.UserRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenHandler implements Command.CommandHandler<RefreshTokenCommand, LoginResult> {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    public LoginResult handle(RefreshTokenCommand cmd) {
        String rawToken = cmd.getRefreshToken();
        if (rawToken == null || rawToken.isBlank()) {
            throw new TokenRefreshException(TokenMessageConstants.INVALID, 401, "INVALID_TOKEN");
        }

        // 1. Validate and rotate refresh token atomically in Redis
        RotationResult rotationResult = tokenService.validateAndRotateRefreshToken(rawToken);

        if (rotationResult.status() == RotationStatus.NOT_FOUND) {
            throw new TokenRefreshException(TokenMessageConstants.INVALID, 401, "INVALID_TOKEN");
        }

        UUID userId = rotationResult.userId();
        UUID deviceId = rotationResult.deviceId();

        if (rotationResult.status() == RotationStatus.BREACH) {
            // Breach detected: delete user session and throw 403 Forbidden
            tokenService.removeUserSession(userId);
            throw new TokenRefreshException(TokenMessageConstants.REUSE_DETECTED, 403, "TOKEN_BREACH");
        }

        // 2. Happy Path: rotation success
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TokenRefreshException(TokenMessageConstants.INVALID, 401, "INVALID_TOKEN"));

        // Generate matching JTIs for the new token pair
        String newJti = UuidCreator.getTimeOrderedEpoch().toString();

        String newAccessToken = tokenService.generateAccessToken(userId, user.getUsername(), newJti);
        String newRefreshToken = tokenService.generateRefreshToken(userId, deviceId, newJti, rotationResult.oldTokenId());

        log.info("Token rotated successfully for user: {}, device: {}", user.getUsername(), deviceId);

        return LoginResult.builder()
                .mfaRequired(false)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
