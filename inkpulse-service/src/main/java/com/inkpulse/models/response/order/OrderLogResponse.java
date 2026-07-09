package com.inkpulse.models.response.order;

public record OrderLogResponse(
        String logId,
        String fromStatus,
        String toStatus,
        String changedBy,
        String adminNote,
        String userNote,
        String createdAt
) {
}
