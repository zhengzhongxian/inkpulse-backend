package com.inkpulse.service.ai.impl;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.grpc.ai.ChatRequest;
import com.inkpulse.grpc.ai.ChatResponse;
import com.inkpulse.grpc.ai.ChatServiceGrpc;
import com.inkpulse.service.ai.IAIChatGrpcService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AIChatGrpcService implements IAIChatGrpcService {

    @Value("${" + KeyConstants.AI_VISION_GRPC_HOST + ":ai-service}")
    private String grpcHost;

    @Value("${" + KeyConstants.AI_VISION_GRPC_PORT + ":50051}")
    private int grpcPort;

    private ManagedChannel channel;
    private ChatServiceGrpc.ChatServiceStub asyncStub;

    @PostConstruct
    public void init() {
        log.info("Initializing AI Chat gRPC async client to {}:{}", grpcHost, grpcPort);
        channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();
        asyncStub = ChatServiceGrpc.newStub(channel);
    }

    @Override
    public void streamChat(String userId, String message, StreamObserver<ChatResponse> responseObserver) {
        log.info("Sending gRPC chat stream request for userId: {}", userId);
        ChatRequest request = ChatRequest.newBuilder()
                .setUserId(userId)
                .setMessage(message)
                .build();

        asyncStub.chatStream(request, responseObserver);
    }

    @PreDestroy
    public void destroy() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("Interrupted while shutting down AI Chat gRPC channel");
                Thread.currentThread().interrupt();
            }
        }
    }
}
