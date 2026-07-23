package com.inkpulse.features.voucher.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Voucher;
import com.inkpulse.features.voucher.queries.GetPublicVouchersQuery;
import com.inkpulse.features.voucher.specifications.VoucherSpecifications;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.voucher.PublicVoucherResponse;
import com.inkpulse.repositories.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.entities.User;
import com.inkpulse.entities.UserProfile;
import com.inkpulse.repositories.UserRepository;
import com.inkpulse.repositories.UserVoucherRepository;
import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPublicVouchersQueryHandler implements Query.QueryHandler<GetPublicVouchersQuery, PagedList<PublicVoucherResponse>> {

    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Override
    @Transactional(readOnly = true)
    public PagedList<PublicVoucherResponse> handle(GetPublicVouchersQuery query) {
        log.info("Handling GetPublicVouchersQuery with search: {}, targetType: {}", query.getSearchKeyword(), query.getTargetType());

        Specification<Voucher> spec = Specification.where(VoucherSpecifications.isCurrentlyValid())
                .and(VoucherSpecifications.hasKeyword(query.getSearchKeyword()))
                .and(VoucherSpecifications.hasTargetType(query.getTargetType()))
                .and(VoucherSpecifications.coinCostLessThanOrEqual(query.getMaxCoinCost()))
                .and(VoucherSpecifications.hasDiscountType(query.getDiscountType()))
                .and(VoucherSpecifications.discountValueBetween(query.getMinDiscountValue(), query.getMaxDiscountValue()))
                .and(VoucherSpecifications.minOrderValueBetween(query.getMinMinOrderValue(), query.getMaxMinOrderValue()));

        if (Boolean.TRUE.equals(query.getSuitableOnly()) && query.getUserId() != null) {
            User user = userRepository.findById(query.getUserId()).orElse(null);
            if (user != null) {
                UserProfile profile = user.getProfile();
                long dbBalance = profile != null && profile.getCoinBalance() != null ? profile.getCoinBalance() : 0L;
                String coinDeltaKey = cacheProperties.buildKey(KeyConstants.SECTION_COIN_PENDING_DELTAS, "");
                String pendingDeltaStr = cacheService.hashGet(coinDeltaKey, user.getId().toString());
                long pendingDelta = pendingDeltaStr != null ? Long.parseLong(pendingDeltaStr) : 0L;
                long realTimeBalance = dbBalance + pendingDelta;

                spec = spec.and(VoucherSpecifications.coinCostLessThanOrEqual((int) realTimeBalance));

                List<UUID> fullyAcquiredVoucherIds = userVoucherRepository.findFullyAcquiredVoucherIds(user.getId());
                if (!fullyAcquiredVoucherIds.isEmpty()) {
                    spec = spec.and(VoucherSpecifications.idNotIn(fullyAcquiredVoucherIds));
                }
            }
        }

        Page<Voucher> page = voucherRepository.findAll(spec, query.toPageable());

        return PagedList.fromPage(page, this::toPublicResponse);
    }

    private PublicVoucherResponse toPublicResponse(Voucher voucher) {
        return new PublicVoucherResponse(
                voucher.getId().toString(),
                voucher.getStartDate().toString(),
                voucher.getEndDate().toString(),
                voucher.getVoucherCode(),
                voucher.getDescription(),
                voucher.getDiscountType().name(),
                voucher.getDiscountValue().stripTrailingZeros().toPlainString(),
                voucher.getMinOrderValue().stripTrailingZeros().toPlainString(),
                voucher.getMaxUses(),
                voucher.getUsedCount(),
                voucher.getMaxUsesPerUser(),
                voucher.getCoinCost(),
                voucher.getTargetType().name(),
                voucher.getMaxDiscountAmount() != null ? voucher.getMaxDiscountAmount().stripTrailingZeros().toPlainString() : null
        );
    }
}
