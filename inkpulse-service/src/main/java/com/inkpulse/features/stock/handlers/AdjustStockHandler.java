package com.inkpulse.features.stock.handlers;

import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.constants.message.StockMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.StockTransaction;
import com.inkpulse.entities.enums.StockTransactionType;
import com.inkpulse.features.book.dto.BookEditionSyncHelper;
import com.inkpulse.features.book.dto.SyncBookEditionMessage;
import com.inkpulse.features.stock.commands.AdjustStockCommand;
import com.inkpulse.repositories.BookEditionRepository;
import com.inkpulse.repositories.StockTransactionRepository;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdjustStockHandler implements Command.CommandHandler<AdjustStockCommand, Void> {

    private final BookEditionRepository bookEditionRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final BookEditionSyncHelper bookEditionSyncHelper;
    private final OutboxPublisher outboxPublisher;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Override
    @Transactional
    public Void handle(AdjustStockCommand command) {
        log.info("Handling AdjustStockCommand for edition: {}, target qty: {}", command.getEditionId(), command.getNewQuantity());

        if (command.getNewQuantity() < 0) {
            throw new BusinessValidationException(StockMessageConstants.INVALID_QUANTITY, StockMessageConstants.CODE_INVALID_QUANTITY);
        }

        BookEdition edition = bookEditionRepository.findById(command.getEditionId())
                .orElseThrow(() -> new BusinessValidationException(StockMessageConstants.EDITION_NOT_FOUND, StockMessageConstants.CODE_EDITION_NOT_FOUND));

        int currentQty = edition.getStockQuantity();
        int delta = command.getNewQuantity() - currentQty;

        if (delta == 0) {
            log.info("Adjust stock target matches current stock. No change needed.");
            return null;
        }

        if (delta > 0) {
            bookEditionRepository.incrementStock(edition.getId(), delta);
        } else {
            // delta < 0: decrement stock by the absolute value of delta
            int affected = bookEditionRepository.decrementStock(edition.getId(), -delta);
            if (affected == 0) {
                throw new BusinessValidationException(
                        String.format(StockMessageConstants.STOCK_ADJUST_INSUFFICIENT, command.getNewQuantity()), 
                        StockMessageConstants.CODE_STOCK_ADJUST_FAILED
                );
            }
        }

        // Update local object to sync with ELS correctly
        edition.setStockQuantity(command.getNewQuantity());

        // Save transaction log
        StockTransaction tx = StockTransaction.builder()
                .edition(edition)
                .delta(delta)
                .type(StockTransactionType.ADJUSTMENT)
                .referenceCode("MANUAL_ADJUSTMENT")
                .note(command.getNote())
                .createdBy(command.getAdminUserId())
                .build();
        stockTransactionRepository.save(tx);

        // Sync with Elasticsearch
        SyncBookEditionMessage syncMsg = bookEditionSyncHelper.buildSyncMessage(edition);
        if (syncMsg != null) {
            outboxPublisher.publish(
                    QueueConstants.SYNC_BOOK_EDITION_PARTIAL,
                    syncMsg,
                    "urn:message:InkPulse.Worker.Features.Book.Messages:SyncBookEditionMessage"
            );
            log.info("Published sync message for adjusted edition: {}", edition.getId());
        }

        // Evict Cache
        try {
            String cacheKey = cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL, edition.getId().toString());
            cacheService.remove(cacheKey);
            log.info("Evicted detail cache for adjusted edition: {}", edition.getId());
        } catch (Exception e) {
            log.error("Failed to evict detail cache for edition: {}", edition.getId(), e);
        }

        return null;
    }
}
