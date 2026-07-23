package com.inkpulse.features.flashsale.handlers;

import com.inkpulse.constants.message.FlashSaleMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.features.flashsale.commands.DeleteFlashSaleCommand;
import com.inkpulse.repositories.FlashSaleRepository;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.features.flashsale.dto.SyncFlashSaleToElsMessage;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteFlashSaleHandler implements Command.CommandHandler<DeleteFlashSaleCommand, Void> {

    private final FlashSaleRepository flashSaleRepository;
    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional
    public Void handle(DeleteFlashSaleCommand command) {
        log.info("Handling DeleteFlashSaleCommand for Flash Sale ID: {}", command.getFlashSaleId());

        FlashSale flashSale = flashSaleRepository.findById(command.getFlashSaleId())
                .orElseThrow(() -> new BusinessValidationException(
                        FlashSaleMessageConstants.FLASHSALE_NOT_FOUND,
                        FlashSaleMessageConstants.CODE_FLASHSALE_NOT_FOUND
                ));

        flashSale.setDeleted(true);
        if (flashSale.getItems() != null) {
            for (FlashSaleItem item : flashSale.getItems()) {
                item.setDeleted(true);
            }
        }
        flashSaleRepository.save(flashSale);

        log.info("Flash Sale soft-deleted successfully with ID: {}", flashSale.getId());

        if (flashSale.getItems() != null) {
            for (FlashSaleItem item : flashSale.getItems()) {
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
        }
        return null;
    }
}
