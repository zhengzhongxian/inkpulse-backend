package com.inkpulse.service.ai.impl;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.grpc.ai.ImageAnalysisRequest;
import com.inkpulse.grpc.ai.ImageAnalysisResponse;
import com.inkpulse.grpc.ai.VisionServiceGrpc;
import com.inkpulse.service.ai.IAIVisionGrpcService;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AIVisionGrpcService implements IAIVisionGrpcService {

    @Value("${" + KeyConstants.AI_VISION_GRPC_HOST + ":ai-service}")
    private String grpcHost;

    @Value("${" + KeyConstants.AI_VISION_GRPC_PORT + ":50051}")
    private int grpcPort;

    @Value("${" + KeyConstants.AI_VISION_GRPC_TIMEOUT + ":90}")
    private int timeoutSeconds;

    private ManagedChannel channel;
    private VisionServiceGrpc.VisionServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        log.info("Initializing AI Vision gRPC client to {}:{}", grpcHost, grpcPort);
        channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();
        blockingStub = VisionServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public ImageAnalysisResponse analyzeImage(byte[] imageData, String fileName, String contentType) {
        log.info("Sending gRPC image analysis request for file: {}, size: {} bytes", fileName, imageData.length);
        
        ImageAnalysisRequest request = ImageAnalysisRequest.newBuilder()
                .setImageData(ByteString.copyFrom(imageData))
                .setFileName(fileName != null ? fileName : "image.jpg")
                .setContentType(contentType != null ? contentType : "image/jpeg")
                .build();

        try {
            return blockingStub
                    .withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS)
                    .analyzeImage(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC vision analysis request failed. Status: {}", e.getStatus(), e);
            throw e;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            log.info("Shutting down AI Vision gRPC client channel");
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Error during gRPC channel shutdown", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
