package com.inkpulse.service.ai;

import com.inkpulse.grpc.ai.ChatResponse;
import io.grpc.stub.StreamObserver;

public interface IAIChatGrpcService {
    void streamChat(String userId, String message, StreamObserver<ChatResponse> responseObserver);
}
