package com.inkpulse.features.voucher.strategies;

import com.inkpulse.entities.enums.VoucherTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VoucherTargetStrategyResolver {

    private final List<VoucherTargetStrategy> strategies;

    public VoucherTargetStrategy resolve(VoucherTargetType targetType) {
        return strategies.stream()
                .filter(s -> s.getTargetType() == targetType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported voucher target type: " + targetType));
    }
}
