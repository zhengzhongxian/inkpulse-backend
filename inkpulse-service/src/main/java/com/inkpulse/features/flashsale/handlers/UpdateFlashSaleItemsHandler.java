package com.inkpulse.features.flashsale.handlers;

import com.inkpulse.constants.message.FlashSaleMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.features.flashsale.commands.UpdateFlashSaleItemsCommand;
import com.inkpulse.features.flashsale.commands.UpdateFlashSaleItemsCommand.FlashSaleItemUpdatePayload;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateFlashSaleItemsHandler implements Command.CommandHandler<UpdateFlashSaleItemsCommand, List<FlashSaleItemResponse>> {

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional
    public List<FlashSaleItemResponse> handle(UpdateFlashSaleItemsCommand command) {
        log.info("Handling UpdateFlashSaleItemsCommand for campaign: {} with {} items", command.getFlashSaleId(), command.getItems().size());

        FlashSale flashSale = flashSaleRepository.findById(command.getFlashSaleId())
                .orElseThrow(() -> new BusinessValidationException(
                        FlashSaleMessageConstants.FLASHSALE_NOT_FOUND,
                        FlashSaleMessageConstants.CODE_FLASHSALE_NOT_FOUND
                ));

        List<UUID> itemIds = command.getItems().stream()
                .map(FlashSaleItemUpdatePayload::getFlashSaleItemId)
                .collect(Collectors.toList());

        List<FlashSaleItem> items = flashSaleItemRepository.findByFlashSaleIdAndIdIn(command.getFlashSaleId(), itemIds);
        if (items.size() != itemIds.size()) {
            throw new BusinessValidationException(
                    "Một số sản phẩm Flash Sale không tồn tại hoặc không thuộc chiến dịch này",
                    "INVALID_FLASH_SALE_ITEMS"
            );
        }

        Map<UUID, FlashSaleItem> itemsMap = items.stream()
                .collect(Collectors.toMap(FlashSaleItem::getId, i -> i));

        for (var itemPayload : command.getItems()) {
            FlashSaleItem item = itemsMap.get(itemPayload.getFlashSaleItemId());
            if (itemPayload.getDiscountAmount() != null) {
                item.setDiscountAmount(itemPayload.getDiscountAmount());
            }
            if (itemPayload.getFlashSaleStock() != null) {
                item.setFlashSaleStock(itemPayload.getFlashSaleStock());
            }
        }

        List<FlashSaleItem> savedItems = flashSaleItemRepository.saveAll(items);
        log.info("Batch updated {} Flash Sale Items successfully", savedItems.size());

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
