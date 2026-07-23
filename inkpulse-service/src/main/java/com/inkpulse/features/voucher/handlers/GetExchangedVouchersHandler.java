package com.inkpulse.features.voucher.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.UserVoucher;
import com.inkpulse.entities.Voucher;
import com.inkpulse.features.voucher.queries.GetExchangedVouchersQuery;
import com.inkpulse.features.voucher.specifications.UserVoucherSpecifications;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.voucher.ExchangedVoucherResponse;
import com.inkpulse.models.response.voucher.PublicVoucherResponse;
import com.inkpulse.repositories.UserVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetExchangedVouchersHandler implements Query.QueryHandler<GetExchangedVouchersQuery, PagedList<ExchangedVoucherResponse>> {

    private final UserVoucherRepository userVoucherRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedList<ExchangedVoucherResponse> handle(GetExchangedVouchersQuery query) {
        log.info("Handling GetExchangedVouchersQuery for user: {}", query.getUserId());

        Specification<UserVoucher> spec = Specification.where(UserVoucherSpecifications.hasUserId(query.getUserId()));

        if (Boolean.TRUE.equals(query.getActiveOnly())) {
            spec = spec.and(UserVoucherSpecifications.isActiveOnly());
        } else if (query.getStatus() != null) {
            spec = spec.and(UserVoucherSpecifications.hasStatus(query.getStatus()));
        }

        PageRequest pageable = PageRequest.of(
                query.getPageNumber() - 1,
                query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "acquiredAt")
        );

        Page<UserVoucher> page = userVoucherRepository.findAll(spec, pageable);

        return PagedList.fromPage(page, this::toExchangedResponse);
    }

    private ExchangedVoucherResponse toExchangedResponse(UserVoucher uv) {
        Voucher v = uv.getVoucher();
        PublicVoucherResponse voucherDto = new PublicVoucherResponse(
                v.getId().toString(),
                v.getStartDate().toString(),
                v.getEndDate().toString(),
                v.getVoucherCode(),
                v.getDescription(),
                v.getDiscountType().name(),
                v.getDiscountValue().stripTrailingZeros().toPlainString(),
                v.getMinOrderValue().stripTrailingZeros().toPlainString(),
                v.getMaxUses(),
                v.getUsedCount(),
                v.getMaxUsesPerUser(),
                v.getCoinCost(),
                v.getTargetType().name(),
                v.getMaxDiscountAmount() != null ? v.getMaxDiscountAmount().stripTrailingZeros().toPlainString() : null
        );

        return new ExchangedVoucherResponse(
                uv.getId().toString(),
                uv.getStatus().name(),
                uv.getAcquiredAt().toString(),
                uv.getUsedAt() != null ? uv.getUsedAt().toString() : null,
                voucherDto
        );
    }
}
