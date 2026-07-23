package com.inkpulse.features.voucher.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.voucher.PublicVoucherResponse;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeVoucherCommand implements Command<PublicVoucherResponse> {
    private UUID userId;
    private UUID voucherId;
}
