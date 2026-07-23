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
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.repositories.FlashSaleItemRepository;

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
                revertDecrements(decrementedSales);
                context.reject(FlashSaleMessageConstants.FLASHSALE_NOT_FOUND);
                return;
            }

            FlashSale flashSale = flashSaleItem.getFlashSale();
            if (flashSale == null || !flashSale.getIsActive()) {
                revertDecrements(decrementedSales);
                context.reject(FlashSaleMessageConstants.NOT_IN_PERIOD);
                return;
            }

            ZonedDateTime now = ZonedDateTime.now();
            if (now.isBefore(flashSale.getStartDate()) || now.isAfter(flashSale.getEndDate())) {
                revertDecrements(decrementedSales);
                context.reject(FlashSaleMessageConstants.NOT_IN_PERIOD);
                return;
            }

            // Check if user already purchased
            String buyersKey = KeyConstants.SECTION_FLASHSALE_BUYERS + ":" + fsItemId;
            if (cacheService.sismember(buyersKey, userIdStr)) {
                revertDecrements(decrementedSales);
                context.reject(FlashSaleMessageConstants.ALREADY_PURCHASED);
                return;
            }

            // Atomically decrement stock
            int qty = item.getQuantity();
            Long remaining = cacheService.hashIncrement(KeyConstants.SECTION_FLASHSALE_STOCK, fsItemId.toString(), -qty);
            if (remaining == null || remaining < 0) {
                // Revert this specific decrement
                cacheService.hashIncrement(KeyConstants.SECTION_FLASHSALE_STOCK, fsItemId.toString(), qty);
                // Revert all previous decrements
                revertDecrements(decrementedSales);
                context.reject(FlashSaleMessageConstants.OUT_OF_STOCK);
                return;
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
