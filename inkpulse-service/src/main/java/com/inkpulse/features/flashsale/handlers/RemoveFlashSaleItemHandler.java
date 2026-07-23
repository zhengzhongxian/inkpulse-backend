package com.inkpulse.features.flashsale.handlers;

import com.inkpulse.constants.message.FlashSaleMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.features.flashsale.commands.RemoveFlashSaleItemCommand;
import com.inkpulse.repositories.FlashSaleItemRepository;
import com.inkpulse.repositories.FlashSaleRepository;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.features.flashsale.dto.SyncFlashSaleToElsMessage;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveFlashSaleItemHandler implements Command.CommandHandler<RemoveFlashSaleItemCommand, Void> {

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional
    public Void handle(RemoveFlashSaleItemCommand command) {
        log.info("Handling RemoveFlashSaleItemCommand for campaign: {}, item: {}", command.getFlashSaleId(), command.getFlashSaleItemId());

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

        item.setDeleted(true);
        flashSaleItemRepository.save(item);
        log.info("Flash Sale Item soft-deleted successfully");

        SyncFlashSaleToElsMessage msg = SyncFlashSaleToElsMessage.builder()
                .bookEditionId(item.getBookEdition().getId())
                .flashSalePrice(null)
                .flashSaleItemId(null)
                .build();

        outboxPublisher.publish(
                QueueConstants.SYNC_FLASHSALE_TO_ELS,
                msg,
                "urn:message:InkPulse.Worker.Features.Book.Messages:SyncFlashSaleToElsMessage"
        );

        return null;
    }
}
