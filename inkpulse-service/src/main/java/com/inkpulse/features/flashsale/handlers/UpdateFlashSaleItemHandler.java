package com.inkpulse.features.flashsale.handlers;

import com.inkpulse.constants.message.FlashSaleMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.features.flashsale.commands.UpdateFlashSaleItemCommand;
import com.inkpulse.models.response.flashsale.FlashSaleItemResponse;
import com.inkpulse.repositories.FlashSaleItemRepository;
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
public class UpdateFlashSaleItemHandler implements Command.CommandHandler<UpdateFlashSaleItemCommand, FlashSaleItemResponse> {

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional
    public FlashSaleItemResponse handle(UpdateFlashSaleItemCommand command) {
        log.info("Handling UpdateFlashSaleItemCommand for campaign: {}, item: {}", command.getFlashSaleId(), command.getFlashSaleItemId());

        FlashSale flashSale = flashSaleRepository.findById(command.getFlashSaleId())
                .orElseThrow(() -> new BusinessValidationException(
                        FlashSaleMessageConstants.FLASHSALE_NOT_FOUND,
                        FlashSaleMessageConstants.CODE_FLASHSALE_NOT_FOUND
                ));

        FlashSaleItem item = flashSaleItemRepository.findById(command.getFlashSaleItemId())
                .orElseThrow(() -> new BusinessValidationException(
                        "Không tìm thấy sản phẩm Flash Sale này",
                        "FLASH_SALE_ITEM_NOT_FOUND"
                ));

        if (!item.getFlashSale().getId().equals(flashSale.getId())) {
            throw new BusinessValidationException(
                    "Sản phẩm Flash Sale này không thuộc chiến dịch đã chỉ định",
                    "INVALID_FLASH_SALE_CAMPAIGN"
            );
        }

        if (command.getDiscountAmount() != null) {
            item.setDiscountAmount(command.getDiscountAmount());
        }
        if (command.getFlashSaleStock() != null) {
            item.setFlashSaleStock(command.getFlashSaleStock());
        }

        FlashSaleItem savedItem = flashSaleItemRepository.save(item);
        log.info("Flash Sale Item updated successfully with ID: {}", savedItem.getId());

        ZonedDateTime now = ZonedDateTime.now();
        boolean isRunning = flashSale.getIsActive() &&
                            !now.isBefore(flashSale.getStartDate()) &&
                            !now.isAfter(flashSale.getEndDate());

        SyncFlashSaleToElsMessage msg = SyncFlashSaleToElsMessage.builder()
                .bookEditionId(savedItem.getBookEdition().getId())
                .flashSalePrice(isRunning ? savedItem.getBookEdition().getPrice().subtract(savedItem.getDiscountAmount()) : null)
                .flashSaleItemId(isRunning ? savedItem.getId().toString() : null)
                .build();

        outboxPublisher.publish(
                QueueConstants.SYNC_FLASHSALE_TO_ELS,
                msg,
                "urn:message:InkPulse.Worker.Features.Book.Messages:SyncFlashSaleToElsMessage"
        );

        return toItemResponse(savedItem);
    }

    private FlashSaleItemResponse toItemResponse(FlashSaleItem item) {
        return FlashSaleItemResponse.builder()
                .flashSaleItemId(item.getId().toString())
                .flashSaleId(item.getFlashSale().getId().toString())
                .name(item.getFlashSale().getName())
                .bookEditionId(item.getBookEdition().getId().toString())
                .bookTitle(item.getBookEdition().getBook().getTitle())
                .editionTitle(item.getBookEdition().getIsbn())
                .thumbnailUrl(item.getBookEdition().getThumbnailUrl())
                .originalPrice(item.getBookEdition().getPrice())
                .discountAmount(item.getDiscountAmount())
                .flashSalePrice(item.getBookEdition().getPrice().subtract(item.getDiscountAmount()))
                .flashSaleStock(item.getFlashSaleStock())
                .soldCount(item.getSoldCount())
                .startDate(item.getFlashSale().getStartDate())
                .endDate(item.getFlashSale().getEndDate())
                .build();
    }
}
