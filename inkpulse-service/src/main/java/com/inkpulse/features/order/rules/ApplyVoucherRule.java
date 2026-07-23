package com.inkpulse.features.order.rules;

import com.inkpulse.constants.message.VoucherMessageConstants;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.UserVoucher;
import com.inkpulse.entities.Voucher;
import com.inkpulse.entities.enums.UserVoucherStatus;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.features.voucher.strategies.VoucherTargetStrategy;
import com.inkpulse.features.voucher.strategies.VoucherTargetStrategyResolver;
import com.inkpulse.models.request.order.OrderItemRequest;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.UserVoucherRepository;
import com.inkpulse.repositories.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ApplyVoucherRule implements IEligibilityRule<CreateOrderContext> {

    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final VoucherTargetStrategyResolver strategyResolver;

    @Override
    public int getOrder() {
        return 4; // Run after ValidateOrderItemsRule(1), StockAvailabilityRule(2), AddressValidationRule(3)
    }

    @Override
    public void evaluate(EligibilityContext<CreateOrderContext> context) {
        CreateOrderContext ctx = context.getEntity();
        String voucherCode = ctx.getCommand().getVoucherCode();

        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            return;
        }

        // 1. Find Voucher
        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode.trim())
                .orElse(null);
        if (voucher == null) {
            context.reject(VoucherMessageConstants.VOUCHER_NOT_FOUND);
            return;
        }

        // 2. Check Active Status
        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            context.reject(VoucherMessageConstants.VOUCHER_EXPIRED);
            return;
        }

        // 3. Check Validity Date Range
        ZonedDateTime now = ZonedDateTime.now();
        if (now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())) {
            context.reject(VoucherMessageConstants.VOUCHER_EXPIRED);
            return;
        }

        // 4. Check User Ownership & Unused status
        UUID userId = ctx.getCommand().getUserId();
        UserVoucher userVoucher = userVoucherRepository
                .findFirstByUserIdAndVoucherIdAndStatus(userId, voucher.getId(), UserVoucherStatus.UNUSED)
                .orElse(null);
        if (userVoucher == null) {
            context.reject(VoucherMessageConstants.VOUCHER_NOT_OWNED_OR_USED);
            return;
        }

        // 5. Check target eligibility and minimum order value
        VoucherTargetType targetType = voucher.getTargetType();
        if (targetType == VoucherTargetType.SHIPPING) {
            // Shipping type applies to the whole order total to check minOrderValue
            BigDecimal totalOrderSubtotal = BigDecimal.ZERO;
            for (OrderItemRequest item : ctx.getCommand().getItems()) {
                BookEdition edition = ctx.getEditions().get(item.getEditionId());
                if (edition != null) {
                    BigDecimal sub = edition.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    totalOrderSubtotal = totalOrderSubtotal.add(sub);
                }
            }
            if (totalOrderSubtotal.compareTo(voucher.getMinOrderValue()) < 0) {
                context.reject(VoucherMessageConstants.VOUCHER_SHIPPING_MIN_ORDER_VALUE_NOT_MET);
                return;
            }
        } else {
            // Item-level target type
            VoucherTargetStrategy strategy = strategyResolver.resolve(targetType);
            BigDecimal eligibleItemsSubtotal = BigDecimal.ZERO;
            boolean hasEligibleItem = false;

            for (OrderItemRequest item : ctx.getCommand().getItems()) {
                BookEdition edition = ctx.getEditions().get(item.getEditionId());
                if (edition != null && strategy.isEligible(voucher, edition)) {
                    hasEligibleItem = true;
                    BigDecimal sub = edition.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    eligibleItemsSubtotal = eligibleItemsSubtotal.add(sub);
                }
            }

            if (!hasEligibleItem) {
                context.reject(VoucherMessageConstants.VOUCHER_NO_ELIGIBLE_ITEMS);
                return;
            }

            if (eligibleItemsSubtotal.compareTo(voucher.getMinOrderValue()) < 0) {
                context.reject(VoucherMessageConstants.VOUCHER_MIN_ORDER_VALUE_NOT_MET);
                return;
            }
        }

        // Set context parameters if successful
        ctx.setAppliedVoucher(voucher);
        ctx.setUserVoucherLink(userVoucher);
    }
}
