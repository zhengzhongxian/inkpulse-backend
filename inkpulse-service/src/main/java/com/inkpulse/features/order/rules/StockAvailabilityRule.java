package com.inkpulse.features.order.rules;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.models.request.order.OrderItemRequest;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.BookEditionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StockAvailabilityRule implements IEligibilityRule<CreateOrderContext> {

    private final BookEditionRepository bookEditionRepository;

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public void evaluate(EligibilityContext<CreateOrderContext> context) {
        CreateOrderContext ctx = context.getEntity();
        Map<UUID, BookEdition> editions = new HashMap<>();

        for (OrderItemRequest item : ctx.getCommand().getItems()) {
            Optional<BookEdition> editionOpt = bookEditionRepository.findById(item.getEditionId());
            if (editionOpt.isEmpty()) {
                context.reject(OrderMessageConstants.EDITION_NOT_FOUND);
                return;
            }

            BookEdition edition = editionOpt.get();
            if (edition.getStockQuantity() < item.getQuantity()) {
                context.reject(String.format(OrderMessageConstants.STOCK_INSUFFICIENT, 
                    edition.getBook() != null ? edition.getBook().getTitle() : edition.getIsbn(), 
                    edition.getStockQuantity()));
                return;
            }
            editions.put(item.getEditionId(), edition);
        }

        ctx.setEditions(editions);
    }
}
