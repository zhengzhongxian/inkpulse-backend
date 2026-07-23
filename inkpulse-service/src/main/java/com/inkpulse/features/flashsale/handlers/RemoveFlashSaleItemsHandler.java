package com.inkpulse.features.flashsale.handlers;

import com.inkpulse.constants.message.FlashSaleMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.features.flashsale.commands.RemoveFlashSaleItemsCommand;
import com.inkpulse.repositories.FlashSaleItemRepository;
import com.inkpulse.repositories.FlashSaleRepository;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.features.flashsale.dto.SyncFlashSaleToElsMessage;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveFlashSaleItemsHandler implements Command.CommandHandler<RemoveFlashSaleItemsCommand, Void> {

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional
    public Void handle(RemoveFlashSaleItemsCommand command) {
        log.info("Handling RemoveFlashSaleItemsCommand for campaign: {} with {} items", command.getFlashSaleId(), command.getItemIds().size());

        FlashSale flashSale = flashSaleRepository.findById(command.getFlashSaleId())
                .orElseThrow(() -> new BusinessValidationException(
                        FlashSaleMessageConstants.FLASHSALE_NOT_FOUND,
                        FlashSaleMessageConstants.CODE_FLASHSALE_NOT_FOUND
                ));

        List<FlashSaleItem> items = flashSaleItemRepository.findByFlashSaleIdAndIdIn(command.getFlashSaleId(), command.getItemIds());
        if (items.size() != command.getItemIds().size()) {
            throw new BusinessValidationException(
                    "Một số sản phẩm Flash Sale không tồn tại hoặc không thuộc chiến dịch này",
                    "INVALID_FLASH_SALE_ITEMS"
            );
        }

        for (FlashSaleItem item : items) {
            item.setDeleted(true);
        }

        flashSaleItemRepository.saveAll(items);
        log.info("Batch soft-deleted {} Flash Sale Items successfully", items.size());

        for (FlashSaleItem item : items) {
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
        }

        return null;
    }
}
