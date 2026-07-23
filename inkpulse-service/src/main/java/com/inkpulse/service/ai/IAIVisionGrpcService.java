package com.inkpulse.service.ai;

import com.inkpulse.grpc.ai.ImageAnalysisResponse;

public interface IAIVisionGrpcService {
    /**
     * Sends image bytes to the AI service via gRPC for verification.
     *
     * @param imageData   binary bytes of the image
     * @param fileName    the original file name
     * @param contentType MIME content type
     * @return analysis result response
     */
    ImageAnalysisResponse analyzeImage(byte[] imageData, String fileName, String contentType);
}
