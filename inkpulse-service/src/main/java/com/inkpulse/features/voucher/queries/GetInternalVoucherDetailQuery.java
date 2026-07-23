package com.inkpulse.features.voucher.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.voucher.VoucherDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GetInternalVoucherDetailQuery implements Query<VoucherDetailResponse> {
    private UUID voucherId;
}
