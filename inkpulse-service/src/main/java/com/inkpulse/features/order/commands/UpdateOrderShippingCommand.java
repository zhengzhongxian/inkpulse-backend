package com.inkpulse.features.order.commands;

import com.inkpulse.cqrs.Command;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderShippingCommand implements Command<Void> {
    private String orderCode;
    private String adminUserId;
    private String note;
    private String requiredNote;
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
}
