package com.inkpulse.features.voucher.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Voucher;
import com.inkpulse.features.voucher.queries.GetInternalVouchersQuery;
import com.inkpulse.features.voucher.specifications.VoucherSpecifications;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.voucher.VoucherResponse;
import com.inkpulse.repositories.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetInternalVouchersQueryHandler implements Query.QueryHandler<GetInternalVouchersQuery, PagedList<VoucherResponse>> {

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedList<VoucherResponse> handle(GetInternalVouchersQuery query) {
        log.info("Handling GetInternalVouchersQuery");

        Specification<Voucher> spec = Specification.where(VoucherSpecifications.hasKeyword(query.getSearchKeyword()))
                .and(VoucherSpecifications.hasDiscountType(query.getDiscountType()))
                .and(VoucherSpecifications.hasTargetType(query.getTargetType()))
                .and(VoucherSpecifications.discountValueBetween(query.getMinDiscountValue(), query.getMaxDiscountValue()))
                .and(VoucherSpecifications.maxUsesBetween(query.getMinMaxUses(), query.getMaxMaxUses()))
                .and(VoucherSpecifications.coinCostBetween(query.getMinCoinCost(), query.getMaxCoinCost()))
                .and(VoucherSpecifications.createdBetween(query.getCreatedFrom(), query.getCreatedTo()))
                .and(VoucherSpecifications.startDateBetween(query.getStartDateFrom(), query.getStartDateTo()))
                .and(VoucherSpecifications.endDateBetween(query.getEndDateFrom(), query.getEndDateTo()))
                .and(VoucherSpecifications.isActive(query.getIsActive()));

        Page<Voucher> page = voucherRepository.findAll(spec, query.toPageable());

        return PagedList.fromPage(page, this::toResponse);
    }

    private VoucherResponse toResponse(Voucher voucher) {
        return new VoucherResponse(
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
                voucher.getIsActive(),
                voucher.getCoinCost(),
                voucher.getTargetType().name(),
                voucher.getMaxDiscountAmount() != null ? voucher.getMaxDiscountAmount().stripTrailingZeros().toPlainString() : null,
                voucher.getCreatedAt().toString()
        );
    }
}
