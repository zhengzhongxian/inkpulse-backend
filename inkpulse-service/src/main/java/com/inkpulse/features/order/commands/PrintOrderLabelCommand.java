package com.inkpulse.features.order.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.order.PrintOrderLabelResponse;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintOrderLabelCommand implements Command<PrintOrderLabelResponse> {
    private UUID adminUserId;
    private String orderCode;
}
