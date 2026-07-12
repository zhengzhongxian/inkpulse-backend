package com.inkpulse.models.request.refund;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRefundRequest {
    private String accountNumber;
    private String bin;
    private String accountName;
}
