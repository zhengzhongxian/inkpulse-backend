package com.inkpulse.features.stock.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.StockTransaction;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.stock.StockTransactionResponse;
import com.inkpulse.features.stock.queries.GetStockHistoryQuery;
import com.inkpulse.repositories.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetStockHistoryQueryHandler implements Query.QueryHandler<GetStockHistoryQuery, PagedList<StockTransactionResponse>> {

    private final StockTransactionRepository stockTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedList<StockTransactionResponse> handle(GetStockHistoryQuery query) {
        // Adjust for 0-indexed Spring PageRequest
        int pageIndex = Math.max(0, query.getPage() - 1);
        int pageSize = Math.max(1, query.getSize());
        
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);
        Page<StockTransaction> page = stockTransactionRepository.findByEditionIdOrderByCreatedAtDesc(
                query.getEditionId(), pageRequest
        );

        return PagedList.fromPage(page, tx -> StockTransactionResponse.builder()
                .id(tx.getId())
                .editionId(tx.getEdition().getId())
                .isbn(tx.getEdition().getIsbn())
                .bookTitle((tx.getEdition().getBook() != null) ? tx.getEdition().getBook().getTitle() : "")
                .delta(tx.getDelta())
                .type(tx.getType().name())
                .referenceCode(tx.getReferenceCode())
                .note(tx.getNote())
                .createdBy(tx.getCreatedBy())
                .createdAt(tx.getCreatedAt())
                .build());
    }
}
