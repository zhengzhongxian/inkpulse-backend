package com.inkpulse.models.response.refund;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestResponse {
    private String id;
    private String orderId;
    private String orderCode;
    private BigDecimal amount;
    private String status;
    private String reason;
    private String approvedByUsername;
    private String payosRefundId;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
