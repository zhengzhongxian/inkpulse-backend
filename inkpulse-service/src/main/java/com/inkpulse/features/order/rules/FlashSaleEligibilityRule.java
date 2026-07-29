package com.inkpulse.features.order.rules;

import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.FlashSaleMessageConstants;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.models.request.order.OrderItemRequest;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.repositories.FlashSaleItemRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleEligibilityRule implements IEligibilityRule<CreateOrderContext> {

    private final FlashSaleItemRepository flashSaleItemRepository;
    private final ICacheService cacheService;

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public void evaluate(EligibilityContext<CreateOrderContext> context) {
        CreateOrderContext ctx = context.getEntity();
        String userIdStr = ctx.getUser().getId().toString();
        Map<UUID, Integer> decrementedSales = new HashMap<>();

        for (OrderItemRequest item : ctx.getCommand().getItems()) {
            if (item.getFlashSaleItemId() == null) {
                continue;
            }

            UUID fsItemId = item.getFlashSaleItemId();
            FlashSaleItem flashSaleItem = flashSaleItemRepository.findById(fsItemId).orElse(null);
            if (flashSaleItem == null) {
                log.info("FlashSaleItem {} not found. Falling back to regular price for edition {}.", fsItemId, item.getEditionId());
                item.setFlashSaleItemId(null);
                continue;
            }

            FlashSale flashSale = flashSaleItem.getFlashSale();
            if (flashSale == null || !flashSale.getIsActive()) {
                log.info("FlashSale for item {} is inactive. Falling back to regular price for edition {}.", fsItemId, item.getEditionId());
                item.setFlashSaleItemId(null);
                continue;
            }

            ZonedDateTime now = ZonedDateTime.now();
            if (now.isBefore(flashSale.getStartDate()) || now.isAfter(flashSale.getEndDate())) {
                log.info("FlashSale for item {} is outside campaign window. Falling back to regular price for edition {}.", fsItemId, item.getEditionId());
                item.setFlashSaleItemId(null);
                continue;
            }

            // Check if user already purchased
            String buyersKey = KeyConstants.SECTION_FLASHSALE_BUYERS + ":" + fsItemId;
            if (cacheService.sismember(buyersKey, userIdStr)) {
                log.info("User {} already purchased flash sale item {}. Falling back to regular price for edition {}.", userIdStr, fsItemId, item.getEditionId());
                item.setFlashSaleItemId(null);
                continue;
            }

            // Atomically decrement stock
            int qty = item.getQuantity();
            Long remaining = cacheService.hashIncrement(KeyConstants.SECTION_FLASHSALE_STOCK, fsItemId.toString(), -qty);
            if (remaining == null || remaining < 0) {
                // Revert this specific decrement
                cacheService.hashIncrement(KeyConstants.SECTION_FLASHSALE_STOCK, fsItemId.toString(), qty);
                log.info("FlashSale item {} stock depleted. Falling back to regular price for edition {}.", fsItemId, item.getEditionId());
                item.setFlashSaleItemId(null);
                continue;
            }

            // Track this successful decrement
            decrementedSales.put(fsItemId, qty);

            // Save to context
            ctx.getActiveFlashSaleItems().put(item.getEditionId(), flashSaleItem);
            ctx.getItemFlashSaleDiscounts().put(item.getEditionId(), flashSaleItem.getDiscountAmount());
        }
    }

    private void revertDecrements(Map<UUID, Integer> decrementedSales) {
        decrementedSales.forEach((fsItemId, qty) -> {
            cacheService.hashIncrement(KeyConstants.SECTION_FLASHSALE_STOCK, fsItemId.toString(), qty);
        });
    }
}
