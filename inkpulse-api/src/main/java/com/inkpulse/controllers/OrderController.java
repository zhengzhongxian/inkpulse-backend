package com.inkpulse.controllers;

import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.features.order.commands.CalculateShippingFeeCommand;
import com.inkpulse.features.order.commands.CreateOrderCommand;
import com.inkpulse.features.order.commands.ConfirmPackOrderCommand;
import com.inkpulse.features.order.commands.PrintOrderLabelCommand;
import com.inkpulse.models.response.order.PrintOrderLabelResponse;
import com.inkpulse.features.order.queries.GetMyOrdersQuery;
import com.inkpulse.features.order.queries.GetOrderDetailQuery;
import com.inkpulse.features.order.queries.GetInternalOrdersQuery;
import com.inkpulse.features.order.queries.GetInternalOrderDetailQuery;
import com.inkpulse.features.order.queries.GetOrderLogsQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.order.CalculateShippingFeeResponse;
import com.inkpulse.models.response.order.CreateOrderResponse;
import com.inkpulse.models.response.order.ConfirmPackOrderResponse;
import com.inkpulse.models.response.order.OrderDetailResponse;
import com.inkpulse.models.response.order.OrderSummaryResponse;
import com.inkpulse.models.response.order.OrderLogResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import an.awesome.pipelinr.Pipeline;

import java.util.List;
import java.util.UUID;

import com.inkpulse.features.order.commands.ApproveOrderCommand;
import com.inkpulse.features.order.commands.CancelOrderCommand;
import com.inkpulse.features.order.commands.ReturnOrderCommand;
import com.inkpulse.features.order.commands.UpdateOrderShippingCommand;
import com.inkpulse.models.request.order.UpdateShippingRequest;
import org.springframework.security.core.Authentication;

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

    @PostMapping("/{orderCode}/pack")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Orders.PACK + "')")
    public ResponseEntity<ResultRes<ConfirmPackOrderResponse>> confirmPack(
            @AuthenticationPrincipal String adminUserId,
            @PathVariable("orderCode") String orderCode) {
        log.info("Request to pack order: {} by admin: {}", orderCode, adminUserId);
        ConfirmPackOrderCommand command = new ConfirmPackOrderCommand(UUID.fromString(adminUserId), orderCode);
        ConfirmPackOrderResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, OrderMessageConstants.CONFIRM_PACK_SUCCESS, 200));
    }

    @PostMapping("/{orderCode}/approve")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Orders.PACK + "')")
    public ResponseEntity<ResultRes<Void>> approveOrder(
            @AuthenticationPrincipal String adminUserId,
            @PathVariable("orderCode") String orderCode) {
        log.info("Request to approve order: {} by admin: {}", orderCode, adminUserId);
        ApproveOrderCommand command = new ApproveOrderCommand(UUID.fromString(adminUserId), orderCode);
        pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(null, "Duyệt đơn hàng thành công!", 200));
    }

    @PostMapping("/{orderCode}/print")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Orders.PACK + "')")
    public ResponseEntity<ResultRes<PrintOrderLabelResponse>> printOrderLabel(
            @AuthenticationPrincipal String adminUserId,
            @PathVariable("orderCode") String orderCode) {
        log.info("Request to print label for order: {} by admin: {}", orderCode, adminUserId);
        PrintOrderLabelCommand command = new PrintOrderLabelCommand(UUID.fromString(adminUserId), orderCode);
        PrintOrderLabelResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, "Sinh link in vận đơn thành công!", 200));
    }

    @GetMapping("/internal")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Orders.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<PagedList<OrderSummaryResponse>>> getInternalOrders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "minAmount", required = false) java.math.BigDecimal minAmount,
            @RequestParam(value = "maxAmount", required = false) java.math.BigDecimal maxAmount,
            @RequestParam(value = "hasVoucher", required = false) Boolean hasVoucher,
            @RequestParam(value = "hasFlashSale", required = false) Boolean hasFlashSale) {
        log.info("Request by admin to get internal orders: page={}, size={}, search={}, status={}, startDate={}, endDate={}, pm={}, minAmount={}, maxAmount={}, hasVoucher={}, hasFlashSale={}",
                page, size, search, status, startDate, endDate, paymentMethod, minAmount, maxAmount, hasVoucher, hasFlashSale);
        GetInternalOrdersQuery query = new GetInternalOrdersQuery();
        query.setPageNumber(page);
        query.setPageSize(size);
        query.setSearchKeyword(search);
        query.setStatus(status);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setPaymentMethod(paymentMethod);
        query.setMinAmount(minAmount);
        query.setMaxAmount(maxAmount);
        query.setHasVoucher(hasVoucher);
        query.setHasFlashSale(hasFlashSale);
        PagedList<OrderSummaryResponse> response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, OrderMessageConstants.GET_ORDERS_SUCCESS, 200));
    }

    @GetMapping("/internal/{orderId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Orders.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<OrderDetailResponse>> getInternalOrderDetail(
            @PathVariable("orderId") UUID orderId) {
        log.info("Request by admin to get internal order detail: {}", orderId);
        GetInternalOrderDetailQuery query = new GetInternalOrderDetailQuery(orderId);
        OrderDetailResponse response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, OrderMessageConstants.GET_ORDER_DETAIL_SUCCESS, 200));
    }

    @GetMapping("/{orderCode}/logs")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Orders.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<List<OrderLogResponse>>> getOrderLogs(
            @PathVariable("orderCode") String orderCode) {
        log.info("Request by admin to get order logs for code: {}", orderCode);
        GetOrderLogsQuery query = new GetOrderLogsQuery(orderCode);
        List<OrderLogResponse> response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, OrderMessageConstants.GET_ORDER_LOGS_SUCCESS, 200));
    }

    @PostMapping("/{orderCode}/cancel")
    public ResponseEntity<ResultRes<Void>> cancelOrder(
            @AuthenticationPrincipal String userId,
            Authentication authentication,
            @PathVariable("orderCode") String orderCode) {
        log.info("Request to cancel order: {} by user: {}", orderCode, userId);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") 
                            || a.getAuthority().equals("ROLE_EMPLOYEE")
                            || a.getAuthority().equals(PermissionConstants.Orders.PACK));
        
        CancelOrderCommand command = CancelOrderCommand.builder()
                .orderCode(orderCode)
                .userId(userId)
                .isAdmin(isAdmin)
                .build();
        pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(null, OrderMessageConstants.CANCEL_ORDER_SUCCESS, 200));
    }

    @PostMapping("/{orderCode}/return")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Orders.PACK + "')")
    public ResponseEntity<ResultRes<Void>> returnOrder(
            @AuthenticationPrincipal String adminUserId,
            @PathVariable("orderCode") String orderCode) {
        log.info("Request to return order: {} by admin: {}", orderCode, adminUserId);
        ReturnOrderCommand command = ReturnOrderCommand.builder()
                .orderCode(orderCode)
                .adminUserId(adminUserId)
                .build();
        pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(null, OrderMessageConstants.RETURN_ORDER_SUCCESS, 200));
    }

    @PostMapping("/{orderCode}/shipping-update")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Orders.PACK + "')")
    public ResponseEntity<ResultRes<Void>> updateShipping(
            @AuthenticationPrincipal String adminUserId,
            @PathVariable("orderCode") String orderCode,
            @RequestBody UpdateShippingRequest request) {
        log.info("Request to update shipping for order: {} by admin: {}", orderCode, adminUserId);
        UpdateOrderShippingCommand command = UpdateOrderShippingCommand.builder()
                .orderCode(orderCode)
                .adminUserId(adminUserId)
                .note(request.getNote())
                .requiredNote(request.getRequiredNote())
                .weight(request.getWeight())
                .length(request.getLength())
                .width(request.getWidth())
                .height(request.getHeight())
                .build();
        pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(null, OrderMessageConstants.UPDATE_SHIPPING_SUCCESS, 200));
    }
}
