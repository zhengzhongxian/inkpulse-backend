package com.inkpulse.features.refund.commands;

import com.inkpulse.cqrs.Command;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRefundCommand implements Command<Void> {
    private UUID refundRequestId;
    private String adminUserId;
    
    // Optional destination bank details for payout refund
    @Builder.Default
    private String accountNumber = "123456789";
    @Builder.Default
    private String bin = "970415"; // VietinBank default code for testing
    @Builder.Default
    private String accountName = "NGUYEN VAN A";
}
