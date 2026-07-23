package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.FlashSaleMessageConstants;
import com.inkpulse.features.flashsale.commands.CreateFlashSaleCommand;
import com.inkpulse.features.flashsale.commands.UpdateFlashSaleCommand;
import com.inkpulse.features.flashsale.commands.DeleteFlashSaleCommand;
import com.inkpulse.features.flashsale.queries.GetInternalFlashSalesQuery;
import com.inkpulse.features.flashsale.queries.GetFlashSaleByIdQuery;
import com.inkpulse.features.flashsale.queries.GetActiveFlashSalesQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.models.response.flashsale.FlashSaleResponse;
import com.inkpulse.models.response.flashsale.FlashSaleDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.inkpulse.features.flashsale.commands.*;
import com.inkpulse.models.request.flashsale.*;
import com.inkpulse.models.response.flashsale.FlashSaleItemResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/flash-sales")
@RequiredArgsConstructor
public class FlashSaleController {

    private final Pipeline pipeline;

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.FlashSales.CREATE + "')")
    public ResponseEntity<ResultRes<FlashSaleResponse>> createFlashSale(
            @RequestBody CreateFlashSaleCommand command) {
        log.info("REST request by admin to create Flash Sale");
        FlashSaleResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, FlashSaleMessageConstants.CREATE_SUCCESS, 200));
    }

    @PutMapping("/{flashSaleId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.FlashSales.EDIT + "')")
    public ResponseEntity<ResultRes<FlashSaleResponse>> updateFlashSale(
            @PathVariable("flashSaleId") UUID flashSaleId,
            @RequestBody UpdateFlashSaleCommand command) {
        log.info("REST request by admin to update Flash Sale ID: {}", flashSaleId);
        command.setFlashSaleId(flashSaleId);
        FlashSaleResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, FlashSaleMessageConstants.UPDATE_SUCCESS, 200));
    }

    @DeleteMapping("/{flashSaleId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.FlashSales.DELETE + "')")
    public ResponseEntity<ResultRes<Void>> deleteFlashSale(
            @PathVariable("flashSaleId") UUID flashSaleId) {
        log.info("REST request by admin to delete Flash Sale ID: {}", flashSaleId);
        DeleteFlashSaleCommand command = new DeleteFlashSaleCommand(flashSaleId);
        pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(null, FlashSaleMessageConstants.DELETE_SUCCESS, 200));
    }

    @GetMapping("/internal")
    @PreAuthorize("hasAuthority('" + PermissionConstants.FlashSales.VIEW + "')")
    public ResponseEntity<ResultRes<PagedList<FlashSaleResponse>>> getInternalFlashSales(
            GetInternalFlashSalesQuery query) {
        log.info("REST request by admin to get internal Flash Sales list");
        PagedList<FlashSaleResponse> response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, FlashSaleMessageConstants.GET_LIST_SUCCESS, 200));
    }

    @GetMapping("/internal/{flashSaleId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.FlashSales.VIEW + "')")
    public ResponseEntity<ResultRes<FlashSaleDetailResponse>> getInternalFlashSaleDetail(
            @PathVariable("flashSaleId") UUID flashSaleId) {
        log.info("REST request by admin to get internal Flash Sale detail for ID: {}", flashSaleId);
        GetFlashSaleByIdQuery query = new GetFlashSaleByIdQuery(flashSaleId);
        FlashSaleDetailResponse response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, FlashSaleMessageConstants.GET_DETAIL_SUCCESS, 200));
    }

    @GetMapping("/active")
    public ResponseEntity<ResultRes<List<FlashSaleItemResponse>>> getActiveFlashSales() {
        log.info("REST request to get active Flash Sales for homepage");
        GetActiveFlashSalesQuery query = new GetActiveFlashSalesQuery();
        List<FlashSaleItemResponse> response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, FlashSaleMessageConstants.GET_LIST_SUCCESS, 200));
    }

    // ------------------- NEW ITEM-LEVEL ENDPOINTS -------------------

    @PostMapping("/{flashSaleId}/items")
    @PreAuthorize("hasAuthority('" + PermissionConstants.FlashSales.EDIT + "')")
    public ResponseEntity<ResultRes<FlashSaleItemResponse>> addFlashSaleItem(
            @PathVariable("flashSaleId") UUID flashSaleId,
            @RequestBody AddFlashSaleItemCommand command) {
        log.info("REST request to add Flash Sale Item to campaign ID: {}", flashSaleId);
        command.setFlashSaleId(flashSaleId);
        FlashSaleItemResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, "Thêm sản phẩm thành công", 200));
    }

    @PostMapping("/{flashSaleId}/items/batch")
    @PreAuthorize("hasAuthority('" + PermissionConstants.FlashSales.EDIT + "')")
    public ResponseEntity<ResultRes<List<FlashSaleItemResponse>>> addFlashSaleItemsBatch(
            @PathVariable("flashSaleId") UUID flashSaleId,
            @RequestBody AddFlashSaleItemsRequest request) {
        log.info("REST request to add batch items to campaign ID: {}", flashSaleId);
        AddFlashSaleItemsCommand command = AddFlashSaleItemsCommand.builder()
                .flashSaleId(flashSaleId)
                .items(request.getItems())
                .build();
        List<FlashSaleItemResponse> response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, "Thêm danh sách sản phẩm thành công", 200));
    }

    @DeleteMapping("/{flashSaleId}/items/{itemId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.FlashSales.EDIT + "')")
    public ResponseEntity<ResultRes<Void>> removeFlashSaleItem(
            @PathVariable("flashSaleId") UUID flashSaleId,
            @PathVariable("itemId") UUID itemId) {
        log.info("REST request to remove Flash Sale Item ID: {} from campaign ID: {}", itemId, flashSaleId);
        RemoveFlashSaleItemCommand command = RemoveFlashSaleItemCommand.builder()
                .flashSaleId(flashSaleId)
                .flashSaleItemId(itemId)
                .build();
        pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(null, "Xóa sản phẩm thành công", 200));
    }

    @DeleteMapping("/{flashSaleId}/items/batch")
    @PreAuthorize("hasAuthority('" + PermissionConstants.FlashSales.EDIT + "')")
    public ResponseEntity<ResultRes<Void>> removeFlashSaleItemsBatch(
            @PathVariable("flashSaleId") UUID flashSaleId,
            @RequestBody BatchRemoveFlashSaleItemsRequest request) {
        log.info("REST request to batch remove items from campaign ID: {}", flashSaleId);
        RemoveFlashSaleItemsCommand command = RemoveFlashSaleItemsCommand.builder()
                .flashSaleId(flashSaleId)
                .itemIds(request.getItemIds())
                .build();
        pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(null, "Xóa danh sách sản phẩm thành công", 200));
    }

    @PutMapping("/{flashSaleId}/items/{itemId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.FlashSales.EDIT + "')")
    public ResponseEntity<ResultRes<FlashSaleItemResponse>> updateFlashSaleItem(
            @PathVariable("flashSaleId") UUID flashSaleId,
            @PathVariable("itemId") UUID itemId,
            @RequestBody UpdateFlashSaleItemCommand command) {
        log.info("REST request to update Flash Sale Item ID: {} in campaign ID: {}", itemId, flashSaleId);
        command.setFlashSaleId(flashSaleId);
        command.setFlashSaleItemId(itemId);
        FlashSaleItemResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, "Cập nhật sản phẩm thành công", 200));
    }

    @PutMapping("/{flashSaleId}/items/batch")
    @PreAuthorize("hasAuthority('" + PermissionConstants.FlashSales.EDIT + "')")
    public ResponseEntity<ResultRes<List<FlashSaleItemResponse>>> updateFlashSaleItemsBatch(
            @PathVariable("flashSaleId") UUID flashSaleId,
            @RequestBody BatchUpdateFlashSaleItemsRequest request) {
        log.info("REST request to batch update items in campaign ID: {}", flashSaleId);
        UpdateFlashSaleItemsCommand command = UpdateFlashSaleItemsCommand.builder()
                .flashSaleId(flashSaleId)
                .items(request.getItems())
                .build();
        List<FlashSaleItemResponse> response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, "Cập nhật danh sách sản phẩm thành công", 200));
    }
}
