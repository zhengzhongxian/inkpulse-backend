package com.inkpulse.features.flashsale.handlers;

import com.inkpulse.constants.message.FlashSaleMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.features.flashsale.commands.CreateFlashSaleCommand;
import com.inkpulse.models.response.flashsale.FlashSaleResponse;
import com.inkpulse.repositories.BookEditionRepository;
import com.inkpulse.repositories.FlashSaleRepository;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.features.flashsale.dto.SyncFlashSaleToElsMessage;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateFlashSaleHandler implements Command.CommandHandler<CreateFlashSaleCommand, FlashSaleResponse> {

    private final FlashSaleRepository flashSaleRepository;
    private final BookEditionRepository bookEditionRepository;
    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional
    public FlashSaleResponse handle(CreateFlashSaleCommand command) {
        log.info("Handling CreateFlashSaleCommand for campaign: {}", command.getName());

        FlashSale flashSale = FlashSale.builder()
            .name(command.getName())
            .isActive(true)
            .startDate(command.getStartDate())
            .endDate(command.getEndDate())
            .build();

        if (command.getItems() != null) {
            for (var itemPayload : command.getItems()) {
                BookEdition bookEdition = bookEditionRepository.findById(itemPayload.getBookEditionId())
                    .orElseThrow(() -> new BusinessValidationException(
                        FlashSaleMessageConstants.EDITION_NOT_FOUND,
                        FlashSaleMessageConstants.CODE_EDITION_NOT_FOUND
                    ));

                FlashSaleItem item = FlashSaleItem.builder()
                    .flashSale(flashSale)
                    .bookEdition(bookEdition)
                    .discountAmount(itemPayload.getDiscountAmount())
                    .flashSaleStock(itemPayload.getFlashSaleStock())
                    .soldCount(0)
                    .build();

                flashSale.getItems().add(item);
            }
        }

        FlashSale savedFlashSale = flashSaleRepository.save(flashSale);
        log.info("Flash Sale campaign created successfully with ID: {} and {} items", savedFlashSale.getId(), savedFlashSale.getItems().size());

        if (savedFlashSale.getItems() != null) {
            ZonedDateTime now = ZonedDateTime.now();
            boolean isRunning = savedFlashSale.getIsActive() &&
                                !now.isBefore(savedFlashSale.getStartDate()) &&
                                !now.isAfter(savedFlashSale.getEndDate());

            for (FlashSaleItem item : savedFlashSale.getItems()) {
                SyncFlashSaleToElsMessage msg = SyncFlashSaleToElsMessage.builder()
                        .bookEditionId(item.getBookEdition().getId())
                        .flashSalePrice(isRunning ? item.getBookEdition().getPrice().subtract(item.getDiscountAmount()) : null)
                        .flashSaleItemId(isRunning ? item.getId().toString() : null)
                        .build();

                outboxPublisher.publish(
                        QueueConstants.SYNC_FLASHSALE_TO_ELS,
                        msg,
                        "urn:message:InkPulse.Worker.Features.Book.Messages:SyncFlashSaleToElsMessage"
                );
            }
        }

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
