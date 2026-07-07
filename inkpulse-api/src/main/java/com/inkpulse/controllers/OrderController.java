package com.inkpulse.controllers;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.features.order.commands.CalculateShippingFeeCommand;
import com.inkpulse.features.order.commands.CreateOrderCommand;
import com.inkpulse.features.order.queries.GetMyOrdersQuery;
import com.inkpulse.features.order.queries.GetOrderDetailQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.order.CalculateShippingFeeResponse;
import com.inkpulse.models.response.order.CreateOrderResponse;
import com.inkpulse.models.response.order.OrderDetailResponse;
import com.inkpulse.models.response.order.OrderSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import an.awesome.pipelinr.Pipeline;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final Pipeline pipeline;

    @PostMapping("/shipping-fee")
    public ResponseEntity<ResultRes<CalculateShippingFeeResponse>> calculateShippingFee(
            @AuthenticationPrincipal String userIdStr,
            @RequestBody CalculateShippingFeeCommand command) {
        log.info("Request to calculate shipping fee for user: {}", userIdStr);
        command.setUserId(UUID.fromString(userIdStr));
        CalculateShippingFeeResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, OrderMessageConstants.CALCULATE_FEE_SUCCESS, 200));
    }

    @PostMapping
    public ResponseEntity<ResultRes<CreateOrderResponse>> createOrder(
            @AuthenticationPrincipal String userIdStr,
            @RequestBody CreateOrderCommand command) {
        log.info("Request to place order for user: {}", userIdStr);
        command.setUserId(UUID.fromString(userIdStr));
        CreateOrderResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, OrderMessageConstants.CREATE_ORDER_SUCCESS, 200));
    }

    @GetMapping
    public ResponseEntity<ResultRes<PagedList<OrderSummaryResponse>>> getMyOrders(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("Request to get my orders for user: {}, page: {}, size: {}", userIdStr, page, size);
        GetMyOrdersQuery query = new GetMyOrdersQuery(UUID.fromString(userIdStr), page, size);
        PagedList<OrderSummaryResponse> response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, OrderMessageConstants.GET_ORDERS_SUCCESS, 200));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ResultRes<OrderDetailResponse>> getOrderDetail(
            @AuthenticationPrincipal String userIdStr,
            @PathVariable("orderId") UUID orderId) {
        log.info("Request to get order detail: {} for user: {}", orderId, userIdStr);
        GetOrderDetailQuery query = new GetOrderDetailQuery(UUID.fromString(userIdStr), orderId);
        OrderDetailResponse response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, OrderMessageConstants.GET_ORDER_DETAIL_SUCCESS, 200));
    }
}
