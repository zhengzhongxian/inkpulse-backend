package com.inkpulse.features.voucher.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.message.VoucherMessageConstants;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.features.voucher.commands.UpdateVoucherCommand;
import com.inkpulse.features.voucher.dto.VoucherDetailCacheDto;
import com.inkpulse.features.voucher.rules.VoucherEligibilityContext;
import com.inkpulse.models.response.voucher.VoucherResponse;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.EligibilityPipeline;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.*;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateVoucherHandler implements Command.CommandHandler<UpdateVoucherCommand, VoucherResponse> {

    private final VoucherRepository voucherRepository;
    private final VoucherBookRepository voucherBookRepository;
    private final VoucherCategoryRepository voucherCategoryRepository;
    private final VoucherEditionRepository voucherEditionRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final BookEditionRepository bookEditionRepository;
    private final SectionCacheService sectionCache;
    
    private final List<IEligibilityRule<VoucherEligibilityContext>> eligibilityRules;

    @Override
    @Transactional
    public VoucherResponse handle(UpdateVoucherCommand command) {
        log.info("Handling UpdateVoucherCommand for ID: {}", command.getVoucherId());

        // 1. Fetch existing voucher
        Voucher voucher = voucherRepository.findById(command.getVoucherId())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", command.getVoucherId()));

        // 2. If voucher is already used, restrict modifications to sensitive fields
        if (voucher.getUsedCount() > 0) {
            boolean discountTypeChanged = voucher.getDiscountType() != command.getDiscountType();
            boolean discountValueChanged = voucher.getDiscountValue().compareTo(command.getDiscountValue()) != 0;
            boolean targetTypeChanged = voucher.getTargetType() != command.getTargetType();
            boolean coinCostChanged = !voucher.getCoinCost().equals(command.getCoinCost());

            if (discountTypeChanged || discountValueChanged || targetTypeChanged || coinCostChanged) {
                log.warn("Attempted to modify locked fields on already used voucher: {}", voucher.getId());
                throw new BusinessValidationException(
                        VoucherMessageConstants.VOUCHER_ALREADY_USED_CANNOT_MODIFY,
                        VoucherMessageConstants.CODE_VOUCHER_USED_LOCKED
                );
            }
        }

        // 3. Run eligibility rules
        VoucherEligibilityContext ruleCtx = VoucherEligibilityContext.builder()
                .existingVoucherId(voucher.getId())
                .voucherCode(command.getVoucherCode())
                .discountType(command.getDiscountType())
                .discountValue(command.getDiscountValue())
                .minOrderValue(command.getMinOrderValue())
                .maxUses(command.getMaxUses())
                .maxUsesPerUser(command.getMaxUsesPerUser())
                .coinCost(command.getCoinCost())
                .targetType(command.getTargetType())
                .targetIds(command.getTargetIds())
                .maxDiscountAmount(command.getMaxDiscountAmount())
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .build();

        EligibilityPipeline<VoucherEligibilityContext> pipeline = new EligibilityPipeline<>(eligibilityRules);
        EligibilityContext<VoucherEligibilityContext> context = pipeline.run(ruleCtx);

        if (context.isRejected()) {
            log.warn("Update voucher validation rejected: {}", context.getRejectionReason());
            throw new BusinessValidationException(context.getRejectionReason(), VoucherMessageConstants.CODE_INVALID_FIELDS);
        }

        // 4. Update core properties
        voucher.setVoucherCode(command.getVoucherCode().trim());
        voucher.setDescription(command.getDescription());
        voucher.setDiscountType(command.getDiscountType());
        voucher.setDiscountValue(command.getDiscountValue());
        voucher.setMinOrderValue(command.getMinOrderValue());
        voucher.setMaxUses(command.getMaxUses());
        voucher.setMaxUsesPerUser(command.getMaxUsesPerUser());
        voucher.setIsActive(command.getIsActive());
        voucher.setCoinCost(command.getCoinCost());
        voucher.setTargetType(command.getTargetType());
        voucher.setMaxDiscountAmount(command.getMaxDiscountAmount());
        voucher.setStartDate(command.getStartDate());
        voucher.setEndDate(command.getEndDate());

        Voucher updatedVoucher = voucherRepository.save(voucher);

        // 5. Update target mapping relations: delete old mappings and save new ones
        voucherCategoryRepository.deleteByVoucherId(voucher.getId());
        voucherBookRepository.deleteByVoucherId(voucher.getId());
        voucherEditionRepository.deleteByVoucherId(voucher.getId());

        VoucherTargetType targetType = command.getTargetType();
        List<UUID> targetIds = command.getTargetIds();

        if (targetType != VoucherTargetType.ALL && targetIds != null) {
            switch (targetType) {
                case CATEGORY -> {
                    List<Category> categories = categoryRepository.findAllById(targetIds);
                    for (Category category : categories) {
                        VoucherCategory mapping = VoucherCategory.builder()
                                .voucher(updatedVoucher)
                                .category(category)
                                .build();
                        voucherCategoryRepository.save(mapping);
                    }
                }
                case BOOK -> {
                    List<Book> books = bookRepository.findAllById(targetIds);
                    for (Book book : books) {
                        VoucherBook mapping = VoucherBook.builder()
                                .voucher(updatedVoucher)
                                .book(book)
                                .build();
                        voucherBookRepository.save(mapping);
                    }
                }
                case EDITION -> {
                    List<BookEdition> editions = bookEditionRepository.findAllById(targetIds);
                    for (BookEdition edition : editions) {
                        VoucherEdition mapping = VoucherEdition.builder()
                                .voucher(updatedVoucher)
                                .bookEdition(edition)
                                .build();
                        voucherEditionRepository.save(mapping);
                    }
                }
            }
        }

        // 6. Invalidate Cache-Aside Redis cache
        try {
            sectionCache.remove(voucher.getId().toString(), VoucherDetailCacheDto.class);
            log.info("Invalidated cache for updated voucher: {}", voucher.getId());
        } catch (Exception e) {
            log.error("Failed to invalidate cache for updated voucher: {}", voucher.getId(), e);
        }

        log.info("Voucher updated successfully with code: {}", updatedVoucher.getVoucherCode());

        return toResponse(updatedVoucher);
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
