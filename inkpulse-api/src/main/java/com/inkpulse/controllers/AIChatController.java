package com.inkpulse.controllers;

import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.AIMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.grpc.ai.ChatResponse;
import com.inkpulse.service.ai.IAIChatGrpcService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AIChatController {

    private final IAIChatGrpcService aiChatGrpcService;
    private final ICacheService cacheService;

    public record AIChatRequestPayload(String message) {}

    @PostMapping(value = "/api/v1/customer/ai/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAIChat(
            @AuthenticationPrincipal String userId,
            @RequestBody AIChatRequestPayload payload) {

        // 1. Enforce Authentication (Guest Check)
        if (userId == null || userId.isBlank() || "anonymousUser".equalsIgnoreCase(userId)) {
            throw new BusinessValidationException(
                    AIMessageConstants.GUEST_UNAUTHORIZED,
                    AIMessageConstants.CODE_GUEST_UNAUTHORIZED);
        }

        if (payload == null || payload.message() == null || payload.message().isBlank()) {
            throw new BusinessValidationException(
                    AIMessageConstants.MESSAGE_EMPTY,
                    AIMessageConstants.CODE_MESSAGE_EMPTY);
        }

        // 2. Enforce Daily Quota Limit via Redis Cache
        String rateKey = KeyConstants.SECTION_AI_CHAT_RATE + ":" + userId + ":" + LocalDate.now();
        String currentCountStr = cacheService.getString(rateKey);
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
        int maxLimit = 15;

        if (currentCount >= maxLimit) {
            throw new BusinessValidationException(
                    String.format(AIMessageConstants.QUOTA_EXCEEDED, maxLimit),
                    AIMessageConstants.CODE_QUOTA_EXCEEDED);
        }

        // Increment quota counter
        cacheService.setString(rateKey, String.valueOf(currentCount + 1), Duration.ofDays(1));

        // 3. Setup SseEmitter with 3-minute timeout
        SseEmitter emitter = new SseEmitter(180000L);

        aiChatGrpcService.streamChat(userId, payload.message(), new StreamObserver<ChatResponse>() {
            @Override
            public void onNext(ChatResponse value) {
                try {
                    if (value.getIsEnd()) {
                        emitter.send(SseEmitter.event().name("end").data("[DONE]"));
                        emitter.complete();
                    } else {
                        emitter.send(SseEmitter.event().data(value.getText()));
                    }
                } catch (IOException e) {
                    log.error("Failed to send SSE chunk for user {}", userId, e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC Chat stream failed for user {}", userId, t);
                try {
                    emitter.send(SseEmitter.event().name("error").data(AIMessageConstants.CHAT_SERVICE_UNAVAILABLE));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(t);
            }

            @Override
            public void onCompleted() {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        });

        return emitter;
    }
}
