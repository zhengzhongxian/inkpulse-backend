package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.VoucherMessageConstants;
import com.inkpulse.entities.enums.VoucherDiscountType;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.features.voucher.commands.CreateVoucherCommand;
import com.inkpulse.features.voucher.commands.UpdateVoucherCommand;
import com.inkpulse.features.voucher.commands.DeleteVoucherCommand;
import com.inkpulse.features.voucher.queries.GetInternalVouchersQuery;
import com.inkpulse.features.voucher.queries.GetPublicVouchersQuery;
import com.inkpulse.features.voucher.queries.GetInternalVoucherDetailQuery;
import com.inkpulse.features.voucher.queries.GetVoucherDetailQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.models.response.voucher.VoucherResponse;
import com.inkpulse.models.response.voucher.VoucherDetailResponse;
import com.inkpulse.models.response.voucher.PublicVoucherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.inkpulse.entities.enums.UserVoucherStatus;
import com.inkpulse.features.voucher.commands.ExchangeVoucherCommand;
import com.inkpulse.features.voucher.queries.GetExchangedVouchersQuery;
import com.inkpulse.features.voucher.queries.GetCheckoutEligibleVouchersQuery;
import com.inkpulse.models.response.voucher.ExchangedVoucherResponse;
import com.inkpulse.models.response.voucher.CheckoutEligibleVoucherResponse;
import com.inkpulse.models.request.order.OrderItemRequest;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final Pipeline pipeline;

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.Vouchers.CREATE + "')")
    public ResponseEntity<ResultRes<VoucherResponse>> createVoucher(
            @RequestBody CreateVoucherCommand command) {
        log.info("REST request to create voucher: {}", command.getVoucherCode());
        VoucherResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, VoucherMessageConstants.CREATE_SUCCESS, 200));
    }

    @PutMapping("/{voucherId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Vouchers.EDIT + "')")
    public ResponseEntity<ResultRes<VoucherResponse>> updateVoucher(
            @PathVariable("voucherId") UUID voucherId,
            @RequestBody UpdateVoucherCommand command) {
        log.info("REST request to update voucher ID: {}", voucherId);
        command.setVoucherId(voucherId);
        VoucherResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, VoucherMessageConstants.UPDATE_SUCCESS, 200));
    }

    @DeleteMapping("/{voucherId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Vouchers.DELETE + "')")
    public ResponseEntity<ResultRes<Void>> deleteVoucher(
            @PathVariable("voucherId") UUID voucherId) {
        log.info("REST request to delete voucher ID: {}", voucherId);
        DeleteVoucherCommand command = new DeleteVoucherCommand(voucherId);
        pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(null, VoucherMessageConstants.DELETE_SUCCESS, 200));
    }

    @GetMapping("/internal")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Vouchers.VIEW + "')")
    public ResponseEntity<ResultRes<PagedList<VoucherResponse>>> getInternalVouchers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "discountType", required = false) VoucherDiscountType discountType,
            @RequestParam(value = "targetType", required = false) VoucherTargetType targetType,
            @RequestParam(value = "minDiscount", required = false) BigDecimal minDiscount,
            @RequestParam(value = "maxDiscount", required = false) BigDecimal maxDiscount,
            @RequestParam(value = "minUses", required = false) Integer minUses,
            @RequestParam(value = "maxUses", required = false) Integer maxUses,
            @RequestParam(value = "minCoinCost", required = false) Integer minCoinCost,
            @RequestParam(value = "maxCoinCost", required = false) Integer maxCoinCost,
            @RequestParam(value = "createdFrom", required = false) String createdFromStr,
            @RequestParam(value = "createdTo", required = false) String createdToStr,
            @RequestParam(value = "startDateFrom", required = false) String startDateFromStr,
            @RequestParam(value = "startDateTo", required = false) String startDateToStr,
            @RequestParam(value = "endDateFrom", required = false) String endDateFromStr,
            @RequestParam(value = "endDateTo", required = false) String endDateToStr,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {

        log.info("REST request by admin to get internal vouchers list");
        GetInternalVouchersQuery query = new GetInternalVouchersQuery();
        query.setPageNumber(page);
        query.setPageSize(size);
        query.setSearchKeyword(search);
        query.setDiscountType(discountType);
        query.setTargetType(targetType);
        query.setMinDiscountValue(minDiscount);
        query.setMaxDiscountValue(maxDiscount);
        query.setMinMaxUses(minUses);
        query.setMaxMaxUses(maxUses);
        query.setMinCoinCost(minCoinCost);
        query.setMaxCoinCost(maxCoinCost);

        if (createdFromStr != null && !createdFromStr.isEmpty()) {
            query.setCreatedFrom(java.time.LocalDateTime.parse(createdFromStr));
        }
        if (createdToStr != null && !createdToStr.isEmpty()) {
            query.setCreatedTo(java.time.LocalDateTime.parse(createdToStr));
        }
        if (startDateFromStr != null && !startDateFromStr.isEmpty()) {
            query.setStartDateFrom(java.time.ZonedDateTime.parse(startDateFromStr));
        }
        if (startDateToStr != null && !startDateToStr.isEmpty()) {
            query.setStartDateTo(java.time.ZonedDateTime.parse(startDateToStr));
        }
        if (endDateFromStr != null && !endDateFromStr.isEmpty()) {
            query.setEndDateFrom(java.time.ZonedDateTime.parse(endDateFromStr));
        }
        if (endDateToStr != null && !endDateToStr.isEmpty()) {
            query.setEndDateTo(java.time.ZonedDateTime.parse(endDateToStr));
        }

        query.setIsActive(isActive);

        PagedList<VoucherResponse> response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, VoucherMessageConstants.GET_LIST_SUCCESS, 200));
    }

    @GetMapping("/internal/{voucherId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Vouchers.VIEW + "')")
    public ResponseEntity<ResultRes<VoucherDetailResponse>> getInternalVoucherDetail(
            @PathVariable("voucherId") UUID voucherId) {
        log.info("REST request by admin to get internal voucher detail for ID: {}", voucherId);
        GetInternalVoucherDetailQuery query = new GetInternalVoucherDetailQuery(voucherId);
        VoucherDetailResponse response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, VoucherMessageConstants.GET_DETAIL_SUCCESS, 200));
    }

    @GetMapping("/{voucherId}")
    public ResponseEntity<ResultRes<VoucherDetailResponse>> getPublicVoucherDetail(
            @PathVariable("voucherId") UUID voucherId) {
        log.info("REST request to get public voucher detail for ID: {}", voucherId);
        GetVoucherDetailQuery query = new GetVoucherDetailQuery(voucherId);
        VoucherDetailResponse response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, VoucherMessageConstants.GET_DETAIL_SUCCESS, 200));
    }

    @GetMapping
    public ResponseEntity<ResultRes<PagedList<PublicVoucherResponse>>> getPublicVouchers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "targetType", required = false) VoucherTargetType targetType,
            @RequestParam(value = "maxCoinCost", required = false) Integer maxCoinCost,
            @RequestParam(value = "suitableOnly", defaultValue = "false") Boolean suitableOnly,
            @RequestParam(value = "discountType", required = false) VoucherDiscountType discountType,
            @RequestParam(value = "minDiscount", required = false) BigDecimal minDiscount,
            @RequestParam(value = "maxDiscount", required = false) BigDecimal maxDiscount,
            @RequestParam(value = "minMinOrder", required = false) BigDecimal minMinOrder,
            @RequestParam(value = "maxMinOrder", required = false) BigDecimal maxMinOrder,
            @AuthenticationPrincipal String userIdStr) {

        log.info("REST request to get public vouchers list, suitableOnly: {}, user: {}", suitableOnly, userIdStr);
        GetPublicVouchersQuery query = new GetPublicVouchersQuery();
        query.setPageNumber(page);
        query.setPageSize(size);
        query.setSearchKeyword(search);
        query.setTargetType(targetType);
        query.setMaxCoinCost(maxCoinCost);
        query.setSuitableOnly(suitableOnly);
        if (userIdStr != null && !userIdStr.isEmpty()) {
            query.setUserId(UUID.fromString(userIdStr));
        }
        query.setDiscountType(discountType);
        query.setMinDiscountValue(minDiscount);
        query.setMaxDiscountValue(maxDiscount);
        query.setMinMinOrderValue(minMinOrder);
        query.setMaxMinOrderValue(maxMinOrder);

        PagedList<PublicVoucherResponse> response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, VoucherMessageConstants.GET_LIST_SUCCESS, 200));
    }

    @PostMapping("/{voucherId}/exchange")
    public ResponseEntity<ResultRes<PublicVoucherResponse>> exchangeVoucher(
            @PathVariable("voucherId") UUID voucherId,
            @AuthenticationPrincipal String userIdStr) {
        log.info("REST request to exchange voucher ID: {} for user: {}", voucherId, userIdStr);
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new BusinessValidationException(VoucherMessageConstants.LOGIN_REQUIRED, VoucherMessageConstants.CODE_UNAUTHORIZED);
        }
        ExchangeVoucherCommand command = new ExchangeVoucherCommand(UUID.fromString(userIdStr), voucherId);
        PublicVoucherResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, VoucherMessageConstants.EXCHANGE_SUCCESS, 200));
    }

    @GetMapping("/my-vouchers")
    public ResponseEntity<ResultRes<PagedList<ExchangedVoucherResponse>>> getMyVouchers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) UserVoucherStatus status,
            @RequestParam(value = "activeOnly", defaultValue = "false") Boolean activeOnly,
            @AuthenticationPrincipal String userIdStr) {
        log.info("REST request to get exchanged vouchers for user: {}, status: {}, activeOnly: {}", userIdStr, status, activeOnly);
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new BusinessValidationException(VoucherMessageConstants.LOGIN_REQUIRED, VoucherMessageConstants.CODE_UNAUTHORIZED);
        }
        GetExchangedVouchersQuery query = new GetExchangedVouchersQuery();
        query.setPageNumber(page);
        query.setPageSize(size);
        query.setUserId(UUID.fromString(userIdStr));
        query.setStatus(status);
        query.setActiveOnly(activeOnly);

        PagedList<ExchangedVoucherResponse> response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, VoucherMessageConstants.GET_LIST_SUCCESS, 200));
    }

    @PostMapping("/checkout-eligibility")
    public ResponseEntity<ResultRes<List<CheckoutEligibleVoucherResponse>>> getCheckoutEligibility(
            @RequestBody List<OrderItemRequest> items,
            @AuthenticationPrincipal String userIdStr) {
        log.info("REST request to check voucher eligibility for checkout. User: {}", userIdStr);
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new BusinessValidationException(VoucherMessageConstants.LOGIN_REQUIRED, VoucherMessageConstants.CODE_UNAUTHORIZED);
        }
        GetCheckoutEligibleVouchersQuery query = new GetCheckoutEligibleVouchersQuery();
        query.setUserId(UUID.fromString(userIdStr));
        query.setItems(items);
        List<CheckoutEligibleVoucherResponse> response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, VoucherMessageConstants.GET_LIST_SUCCESS, 200));
    }
}
