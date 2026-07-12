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
import com.inkpulse.features.stock.commands.ImportStockCommand;
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
public class ImportStockHandler implements Command.CommandHandler<ImportStockCommand, Void> {

    private final BookEditionRepository bookEditionRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final BookEditionSyncHelper bookEditionSyncHelper;
    private final OutboxPublisher outboxPublisher;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Override
    @Transactional
    public Void handle(ImportStockCommand command) {
        log.info("Handling ImportStockCommand for edition: {}, qty: {}", command.getEditionId(), command.getQuantity());

        if (command.getQuantity() <= 0) {
            throw new BusinessValidationException(StockMessageConstants.INVALID_QUANTITY, StockMessageConstants.CODE_INVALID_QUANTITY);
        }

        BookEdition edition = bookEditionRepository.findById(command.getEditionId())
                .orElseThrow(() -> new BusinessValidationException(StockMessageConstants.EDITION_NOT_FOUND, StockMessageConstants.CODE_EDITION_NOT_FOUND));

        // Increment stock in DB atomically
        bookEditionRepository.incrementStock(edition.getId(), command.getQuantity());

        // Update local object to sync with ELS correctly
        edition.setStockQuantity(edition.getStockQuantity() + command.getQuantity());

        // Save transaction log
        StockTransaction tx = StockTransaction.builder()
                .edition(edition)
                .delta(command.getQuantity())
                .type(StockTransactionType.IMPORT)
                .referenceCode("MANUAL_IMPORT")
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
            log.info("Published sync message for imported edition: {}", edition.getId());
        }

        // Evict Cache
        try {
            String cacheKey = cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL, edition.getId().toString());
            cacheService.remove(cacheKey);
            log.info("Evicted detail cache for imported edition: {}", edition.getId());
        } catch (Exception e) {
            log.error("Failed to evict detail cache for edition: {}", edition.getId(), e);
        }

        return null;
    }
}
