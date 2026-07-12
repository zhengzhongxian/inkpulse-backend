package com.inkpulse.features.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnStatusUpdateMessage {
    private String orderCode;    // GHN order code (e.g. Z82BS)
    private String status;       // ready_to_pick, delivering, delivered, cancel...
    private String rawPayload;   // raw JSON payload from GHN
    private String type;         // switch_status
}
