package com.inkpulse.features.voucher.commands;

import com.inkpulse.cqrs.Command;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteVoucherCommand implements Command<Void> {
    private UUID voucherId;
}
