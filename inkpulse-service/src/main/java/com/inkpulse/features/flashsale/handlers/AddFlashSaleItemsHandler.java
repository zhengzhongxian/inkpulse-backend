package com.inkpulse.features.flashsale.handlers;

import com.inkpulse.constants.message.FlashSaleMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.features.flashsale.commands.AddFlashSaleItemsCommand;
import com.inkpulse.models.request.flashsale.CreateFlashSaleRequest.FlashSaleItemPayload;
import com.inkpulse.models.response.flashsale.FlashSaleItemResponse;
import com.inkpulse.repositories.BookEditionRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddFlashSaleItemsHandler implements Command.CommandHandler<AddFlashSaleItemsCommand, List<FlashSaleItemResponse>> {

    private final FlashSaleRepository flashSaleRepository;
    private final BookEditionRepository bookEditionRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional
    public List<FlashSaleItemResponse> handle(AddFlashSaleItemsCommand command) {
        log.info("Handling AddFlashSaleItemsCommand for campaign: {} with {} items", command.getFlashSaleId(), command.getItems().size());

        FlashSale flashSale = flashSaleRepository.findById(command.getFlashSaleId())
                .orElseThrow(() -> new BusinessValidationException(
                        FlashSaleMessageConstants.FLASHSALE_NOT_FOUND,
                        FlashSaleMessageConstants.CODE_FLASHSALE_NOT_FOUND
                ));

        List<UUID> editionIds = command.getItems().stream()
                .map(FlashSaleItemPayload::getBookEditionId)
                .collect(Collectors.toList());

        List<BookEdition> editionsList = bookEditionRepository.findAllById(editionIds);
        if (editionsList.size() != editionIds.size()) {
            throw new BusinessValidationException(
                    FlashSaleMessageConstants.EDITION_NOT_FOUND,
                    FlashSaleMessageConstants.CODE_EDITION_NOT_FOUND
            );
        }

        Map<UUID, BookEdition> editionsMap = editionsList.stream()
                .collect(Collectors.toMap(BookEdition::getId, e -> e));

        List<FlashSaleItem> newItems = new ArrayList<>();
        for (var itemPayload : command.getItems()) {
            boolean exists = flashSaleItemRepository.existsByFlashSaleIdAndBookEditionId(command.getFlashSaleId(), itemPayload.getBookEditionId());
            if (exists) {
                throw new BusinessValidationException(
                        "Phiên bản sách này đã được thêm vào chiến dịch trước đó",
                        "DUPLICATE_FLASH_SALE_ITEM"
                );
            }

            BookEdition bookEdition = editionsMap.get(itemPayload.getBookEditionId());
            FlashSaleItem item = FlashSaleItem.builder()
                    .flashSale(flashSale)
                    .bookEdition(bookEdition)
                    .discountAmount(itemPayload.getDiscountAmount())
                    .flashSaleStock(itemPayload.getFlashSaleStock())
                    .soldCount(0)
                    .build();

            newItems.add(item);
        }

        List<FlashSaleItem> savedItems = flashSaleItemRepository.saveAll(newItems);
        log.info("Batch added {} Flash Sale Items successfully", savedItems.size());

        ZonedDateTime now = ZonedDateTime.now();
        boolean isRunning = flashSale.getIsActive() &&
                            !now.isBefore(flashSale.getStartDate()) &&
                            !now.isAfter(flashSale.getEndDate());

        for (FlashSaleItem item : savedItems) {
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

        return savedItems.stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
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
