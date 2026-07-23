package com.inkpulse.features.flashsale.handlers;

import com.inkpulse.constants.message.FlashSaleMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.features.flashsale.commands.UpdateFlashSaleCommand;
import com.inkpulse.models.response.flashsale.FlashSaleResponse;
import com.inkpulse.repositories.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateFlashSaleHandler implements Command.CommandHandler<UpdateFlashSaleCommand, FlashSaleResponse> {

    private final FlashSaleRepository flashSaleRepository;

    @Override
    @Transactional
    public FlashSaleResponse handle(UpdateFlashSaleCommand command) {
        log.info("Handling UpdateFlashSaleCommand for Flash Sale ID: {}", command.getFlashSaleId());

        FlashSale flashSale = flashSaleRepository.findById(command.getFlashSaleId())
                .orElseThrow(() -> new BusinessValidationException(
                        FlashSaleMessageConstants.FLASHSALE_NOT_FOUND,
                        FlashSaleMessageConstants.CODE_FLASHSALE_NOT_FOUND
                ));

        if (command.getName() != null) {
            flashSale.setName(command.getName());
        }
        if (command.getIsActive() != null) {
            flashSale.setIsActive(command.getIsActive());
        }
        if (command.getStartDate() != null) {
            flashSale.setStartDate(command.getStartDate());
        }
        if (command.getEndDate() != null) {
            flashSale.setEndDate(command.getEndDate());
        }

        FlashSale savedFlashSale = flashSaleRepository.save(flashSale);

        log.info("Flash Sale campaign updated successfully with ID: {}", savedFlashSale.getId());

        return toResponse(savedFlashSale);
    }

    private FlashSaleResponse toResponse(FlashSale flashSale) {
        return FlashSaleResponse.builder()
                .flashSaleId(flashSale.getId().toString())
                .name(flashSale.getName())
                .itemCount(flashSale.getItems() != null ? flashSale.getItems().size() : 0)
                .isActive(flashSale.getIsActive())
                .startDate(flashSale.getStartDate())
                .endDate(flashSale.getEndDate())
                .createdAt(flashSale.getCreatedAt())
                .build();
    }
}
