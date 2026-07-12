package com.inkpulse.features.stock.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.stock.StockTransactionResponse;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetStockHistoryQuery implements Query<PagedList<StockTransactionResponse>> {
    private UUID editionId;
    private int page;
    private int size;
}
