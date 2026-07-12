package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.StockMessageConstants;
import com.inkpulse.features.stock.commands.ImportStockCommand;
import com.inkpulse.features.stock.commands.AdjustStockCommand;
import com.inkpulse.features.stock.queries.GetStockHistoryQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.request.stock.ImportStockRequest;
import com.inkpulse.models.request.stock.AdjustStockRequest;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.models.response.stock.StockTransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
public class StockController {

    private final Pipeline pipeline;

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Inventory.MANAGE + "')")
    public ResponseEntity<ResultRes<Void>> importStock(
            @AuthenticationPrincipal String adminUserIdStr,
            @RequestBody ImportStockRequest request) {
        log.info("Request to import stock for edition: {} by admin: {}", request.getEditionId(), adminUserIdStr);
        
        ImportStockCommand command = ImportStockCommand.builder()
                .editionId(request.getEditionId())
                .quantity(request.getQuantity())
                .note(request.getNote())
                .adminUserId(UUID.fromString(adminUserIdStr))
                .build();
                
        pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(null, StockMessageConstants.IMPORT_SUCCESS, 200));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Inventory.MANAGE + "')")
    public ResponseEntity<ResultRes<Void>> adjustStock(
            @AuthenticationPrincipal String adminUserIdStr,
            @RequestBody AdjustStockRequest request) {
        log.info("Request to adjust stock for edition: {} by admin: {}", request.getEditionId(), adminUserIdStr);
        
        AdjustStockCommand command = AdjustStockCommand.builder()
                .editionId(request.getEditionId())
                .newQuantity(request.getNewQuantity())
                .note(request.getNote())
                .adminUserId(UUID.fromString(adminUserIdStr))
                .build();
                
        pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(null, StockMessageConstants.ADJUST_SUCCESS, 200));
    }

    @GetMapping("/history/{editionId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Inventory.MANAGE + "')")
    public ResponseEntity<ResultRes<PagedList<StockTransactionResponse>>> getStockHistory(
            @PathVariable("editionId") UUID editionId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("Request to get stock history for edition: {}, page: {}, size: {}", editionId, page, size);
        
        GetStockHistoryQuery query = GetStockHistoryQuery.builder()
                .editionId(editionId)
                .page(page)
                .size(size)
                .build();
                
        PagedList<StockTransactionResponse> response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, StockMessageConstants.GET_HISTORY_SUCCESS, 200));
    }
}
