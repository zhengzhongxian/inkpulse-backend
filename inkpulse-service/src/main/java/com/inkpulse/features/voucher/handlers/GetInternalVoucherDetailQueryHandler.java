package com.inkpulse.features.voucher.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.features.voucher.queries.GetInternalVoucherDetailQuery;
import com.inkpulse.models.response.voucher.VoucherDetailResponse;
import com.inkpulse.repositories.*;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetInternalVoucherDetailQueryHandler implements Query.QueryHandler<GetInternalVoucherDetailQuery, VoucherDetailResponse> {

    private final VoucherRepository voucherRepository;
    private final VoucherBookRepository voucherBookRepository;
    private final VoucherCategoryRepository voucherCategoryRepository;
    private final VoucherEditionRepository voucherEditionRepository;

    @Override
    @Transactional(readOnly = true)
    public VoucherDetailResponse handle(GetInternalVoucherDetailQuery query) {
        log.info("Handling GetInternalVoucherDetailQuery for ID: {}", query.getVoucherId());

        Voucher voucher = voucherRepository.findById(query.getVoucherId())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", query.getVoucherId()));

        List<VoucherDetailResponse.VoucherTargetItemResponse> targetItems = new ArrayList<>();
        VoucherTargetType targetType = voucher.getTargetType();

        if (targetType != VoucherTargetType.ALL) {
            switch (targetType) {
                case CATEGORY -> {
                    List<VoucherCategory> mappings = voucherCategoryRepository.findByVoucherId(voucher.getId());
                    for (VoucherCategory mapping : mappings) {
                        if (mapping.getCategory() != null) {
                            targetItems.add(new VoucherDetailResponse.VoucherTargetItemResponse(
                                    mapping.getCategory().getId().toString(),
                                    mapping.getCategory().getName()
                            ));
                        }
                    }
                }
                case BOOK -> {
                    List<VoucherBook> mappings = voucherBookRepository.findByVoucherId(voucher.getId());
                    for (VoucherBook mapping : mappings) {
                        if (mapping.getBook() != null) {
                            targetItems.add(new VoucherDetailResponse.VoucherTargetItemResponse(
                                    mapping.getBook().getId().toString(),
                                    mapping.getBook().getTitle()
                            ));
                        }
                    }
                }
                case EDITION -> {
                    List<VoucherEdition> mappings = voucherEditionRepository.findByVoucherId(voucher.getId());
                    for (VoucherEdition mapping : mappings) {
                        if (mapping.getBookEdition() != null) {
                            BookEdition edition = mapping.getBookEdition();
                            String editionName = edition.getBook().getTitle() + " - " + 
                                    (edition.getCoverType() != null ? edition.getCoverType().name() : "Standard") + 
                                    " (ISBN: " + edition.getIsbn() + ")";
                            targetItems.add(new VoucherDetailResponse.VoucherTargetItemResponse(
                                    edition.getId().toString(),
                                    editionName
                            ));
                        }
                    }
                }
            }
        }

        return new VoucherDetailResponse(
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
                voucher.getCreatedAt().toString(),
                targetItems
        );
    }
}
