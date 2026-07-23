package com.inkpulse.features.voucher.handlers;

import com.inkpulse.constants.message.VoucherMessageConstants;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.features.voucher.commands.CreateVoucherCommand;
import com.inkpulse.features.voucher.rules.VoucherEligibilityContext;
import com.inkpulse.models.response.voucher.VoucherResponse;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.EligibilityPipeline;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.*;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateVoucherHandler implements Command.CommandHandler<CreateVoucherCommand, VoucherResponse> {

    private final VoucherRepository voucherRepository;
    private final VoucherBookRepository voucherBookRepository;
    private final VoucherCategoryRepository voucherCategoryRepository;
    private final VoucherEditionRepository voucherEditionRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final BookEditionRepository bookEditionRepository;
    
    private final List<IEligibilityRule<VoucherEligibilityContext>> eligibilityRules;

    @Override
    @Transactional
    public VoucherResponse handle(CreateVoucherCommand command) {
        log.info("Handling CreateVoucherCommand for code: {}", command.getVoucherCode());

        // 1. Run eligibility rules
        VoucherEligibilityContext ruleCtx = VoucherEligibilityContext.builder()
                .existingVoucherId(null)
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
            log.warn("Create voucher validation rejected: {}", context.getRejectionReason());
            throw new BusinessValidationException(context.getRejectionReason(), VoucherMessageConstants.CODE_INVALID_FIELDS);
        }

        // 2. Build and save Voucher entity (Hibernate will generate ID automatically)
        Voucher voucher = Voucher.builder()
                .voucherCode(command.getVoucherCode().trim())
                .description(command.getDescription())
                .discountType(command.getDiscountType())
                .discountValue(command.getDiscountValue())
                .minOrderValue(command.getMinOrderValue())
                .maxUses(command.getMaxUses())
                .maxUsesPerUser(command.getMaxUsesPerUser())
                .isActive(command.getIsActive())
                .coinCost(command.getCoinCost())
                .targetType(command.getTargetType())
                .maxDiscountAmount(command.getMaxDiscountAmount())
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .build();

        Voucher savedVoucher = voucherRepository.save(voucher);

        // 3. Create target mappings based on targetType
        VoucherTargetType targetType = command.getTargetType();
        List<UUID> targetIds = command.getTargetIds();

        if (targetType != VoucherTargetType.ALL && targetIds != null) {
            switch (targetType) {
                case CATEGORY -> {
                    List<Category> categories = categoryRepository.findAllById(targetIds);
                    for (Category category : categories) {
                        VoucherCategory mapping = VoucherCategory.builder()
                                .voucher(savedVoucher)
                                .category(category)
                                .build();
                        voucherCategoryRepository.save(mapping);
                    }
                }
                case BOOK -> {
                    List<Book> books = bookRepository.findAllById(targetIds);
                    for (Book book : books) {
                        VoucherBook mapping = VoucherBook.builder()
                                .voucher(savedVoucher)
                                .book(book)
                                .build();
                        voucherBookRepository.save(mapping);
                    }
                }
                case EDITION -> {
                    List<BookEdition> editions = bookEditionRepository.findAllById(targetIds);
                    for (BookEdition edition : editions) {
                        VoucherEdition mapping = VoucherEdition.builder()
                                .voucher(savedVoucher)
                                .bookEdition(edition)
                                .build();
                        voucherEditionRepository.save(mapping);
                    }
                }
            }
        }

        log.info("Voucher created successfully with code: {}", savedVoucher.getVoucherCode());

        return toResponse(savedVoucher);
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
