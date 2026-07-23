package com.inkpulse.features.voucher.handlers;

import com.inkpulse.constants.message.VoucherMessageConstants;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.UserVoucher;
import com.inkpulse.entities.Voucher;
import com.inkpulse.entities.enums.UserVoucherStatus;
import com.inkpulse.entities.enums.VoucherDiscountType;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.features.voucher.queries.GetCheckoutEligibleVouchersQuery;
import com.inkpulse.features.voucher.strategies.VoucherTargetStrategy;
import com.inkpulse.features.voucher.strategies.VoucherTargetStrategyResolver;
import com.inkpulse.models.request.order.OrderItemRequest;
import com.inkpulse.models.response.voucher.CheckoutEligibleVoucherResponse;
import com.inkpulse.models.response.voucher.PublicVoucherResponse;
import com.inkpulse.repositories.BookEditionRepository;
import com.inkpulse.repositories.UserVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetCheckoutEligibleVouchersQueryHandler implements Query.QueryHandler<GetCheckoutEligibleVouchersQuery, List<CheckoutEligibleVoucherResponse>> {

    private final UserVoucherRepository userVoucherRepository;
    private final BookEditionRepository bookEditionRepository;
    private final VoucherTargetStrategyResolver strategyResolver;

    @Override
    @Transactional(readOnly = true)
    public List<CheckoutEligibleVoucherResponse> handle(GetCheckoutEligibleVouchersQuery query) {
        log.info("Handling GetCheckoutEligibleVouchersQuery for user: {}", query.getUserId());

        if (query.getUserId() == null) {
            return Collections.emptyList();
        }

        // 1. Get all UNUSED user vouchers
        List<UserVoucher> userVouchers = userVoucherRepository.findAllByUserId(query.getUserId()).stream()
                .filter(uv -> uv.getStatus() == UserVoucherStatus.UNUSED)
                .toList();

        if (userVouchers.isEmpty() || query.getItems() == null || query.getItems().isEmpty()) {
            return userVouchers.stream()
                    .map(uv -> mapToResponse(uv, false, "Giỏ hàng trống", BigDecimal.ZERO))
                    .toList();
        }

        // 2. Fetch all cart editions
        List<UUID> editionIds = query.getItems().stream()
                .map(OrderItemRequest::getEditionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, BookEdition> editionsMap = bookEditionRepository.findAllById(editionIds).stream()
                .collect(Collectors.toMap(BookEdition::getId, e -> e));

        ZonedDateTime now = ZonedDateTime.now();

        List<CheckoutEligibleVoucherResponse> responses = new ArrayList<>();

        for (UserVoucher uv : userVouchers) {
            Voucher voucher = uv.getVoucher();
            boolean eligible = true;
            String reason = null;
            BigDecimal discountAmount = BigDecimal.ZERO;

            // Check Active
            if (!Boolean.TRUE.equals(voucher.getIsActive())) {
                eligible = false;
                reason = VoucherMessageConstants.VOUCHER_EXPIRED;
            }
            // Check Dates
            else if (now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())) {
                eligible = false;
                reason = VoucherMessageConstants.VOUCHER_EXPIRED;
            } else {
                VoucherTargetType targetType = voucher.getTargetType();
                if (targetType == VoucherTargetType.SHIPPING) {
                    // Check minOrderValue against total cart subtotal
                    BigDecimal totalSubtotal = BigDecimal.ZERO;
                    for (OrderItemRequest item : query.getItems()) {
                        BookEdition edition = editionsMap.get(item.getEditionId());
                        if (edition != null) {
                            totalSubtotal = totalSubtotal.add(edition.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                        }
                    }

                    if (totalSubtotal.compareTo(voucher.getMinOrderValue()) < 0) {
                        eligible = false;
                        reason = VoucherMessageConstants.VOUCHER_SHIPPING_MIN_ORDER_VALUE_NOT_MET;
                    } else {
                        // Calculate discount based on FIXED_AMOUNT or return percentage value
                        if (voucher.getDiscountType() == VoucherDiscountType.FIXED_AMOUNT) {
                            discountAmount = voucher.getDiscountValue();
                        } else {
                            discountAmount = voucher.getDiscountValue();
                        }
                    }
                } else {
                    // Item target checks
                    VoucherTargetStrategy strategy = strategyResolver.resolve(targetType);
                    BigDecimal eligibleItemsSubtotal = BigDecimal.ZERO;
                    boolean hasEligibleItem = false;

                    for (OrderItemRequest item : query.getItems()) {
                        BookEdition edition = editionsMap.get(item.getEditionId());
                        if (edition != null && strategy.isEligible(voucher, edition)) {
                            hasEligibleItem = true;
                            eligibleItemsSubtotal = eligibleItemsSubtotal.add(edition.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                        }
                    }

                    if (!hasEligibleItem) {
                        eligible = false;
                        reason = VoucherMessageConstants.VOUCHER_NO_ELIGIBLE_ITEMS;
                    } else if (eligibleItemsSubtotal.compareTo(voucher.getMinOrderValue()) < 0) {
                        eligible = false;
                        reason = VoucherMessageConstants.VOUCHER_MIN_ORDER_VALUE_NOT_MET;
                    } else {
                        // Calculate discount
                        if (voucher.getDiscountType() == VoucherDiscountType.FIXED_AMOUNT) {
                            discountAmount = voucher.getDiscountValue();
                            if (discountAmount.compareTo(eligibleItemsSubtotal) > 0) {
                                discountAmount = eligibleItemsSubtotal;
                            }
                        } else if (voucher.getDiscountType() == VoucherDiscountType.PERCENTAGE) {
                            BigDecimal calculated = eligibleItemsSubtotal.multiply(voucher.getDiscountValue())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            if (voucher.getMaxDiscountAmount() != null) {
                                calculated = calculated.min(voucher.getMaxDiscountAmount());
                            }
                            discountAmount = calculated;
                        }
                    }
                }
            }

            responses.add(mapToResponse(uv, eligible, reason, discountAmount));
        }

        // Sort: eligible first, then by discount amount descending
        responses.sort((r1, r2) -> {
            if (r1.eligible() != r2.eligible()) {
                return r1.eligible() ? -1 : 1;
            }
            return r2.calculatedDiscount().compareTo(r1.calculatedDiscount());
        });

        return responses;
    }

    private CheckoutEligibleVoucherResponse mapToResponse(UserVoucher uv, boolean eligible, String reason, BigDecimal discountAmount) {
        Voucher v = uv.getVoucher();
        PublicVoucherResponse pvr = new PublicVoucherResponse(
                v.getId().toString(),
                v.getStartDate().toString(),
                v.getEndDate().toString(),
                v.getVoucherCode(),
                v.getDescription(),
                v.getDiscountType().name(),
                v.getDiscountValue().toString(),
                v.getMinOrderValue().toString(),
                v.getMaxUses(),
                v.getUsedCount(),
                v.getMaxUsesPerUser(),
                v.getCoinCost(),
                v.getTargetType().name(),
                v.getMaxDiscountAmount() != null ? v.getMaxDiscountAmount().stripTrailingZeros().toPlainString() : null
        );

        return new CheckoutEligibleVoucherResponse(
                uv.getId().toString(),
                uv.getStatus().name(),
                pvr,
                eligible,
                reason,
                discountAmount
        );
    }
}
