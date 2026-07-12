package com.inkpulse.controllers;

import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.features.refund.commands.ApproveRefundCommand;
import com.inkpulse.features.refund.queries.GetRefundRequestsQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.request.refund.ApproveRefundRequest;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.models.response.refund.RefundRequestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import an.awesome.pipelinr.Pipeline;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final Pipeline pipeline;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.Refunds.VIEW + "')")
    public ResponseEntity<ResultRes<PagedList<RefundRequestResponse>>> getRefundRequests(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        log.info("Request by admin to get refund requests: page={}, size={}, search={}, status={}, startDate={}, endDate={}",
                page, size, search, status, startDate, endDate);
        
        GetRefundRequestsQuery query = new GetRefundRequestsQuery();
        query.setPageNumber(page);
        query.setPageSize(size);
        query.setSearchKeyword(search);
        query.setStatus(status);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        
        PagedList<RefundRequestResponse> response = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(response, "Lấy danh sách phiếu hoàn tiền thành công!", 200));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Refunds.APPROVE + "')")
    public ResponseEntity<ResultRes<Void>> approveRefund(
            @AuthenticationPrincipal String adminUserId,
            @PathVariable("id") UUID refundRequestId,
            @RequestBody(required = false) ApproveRefundRequest request) {
        log.info("Request to approve refund request: {} by admin: {}", refundRequestId, adminUserId);
        
        ApproveRefundCommand.ApproveRefundCommandBuilder builder = ApproveRefundCommand.builder()
                .refundRequestId(refundRequestId)
                .adminUserId(adminUserId);
                
        if (request != null) {
            if (request.getAccountNumber() != null) builder.accountNumber(request.getAccountNumber());
            if (request.getBin() != null) builder.bin(request.getBin());
            if (request.getAccountName() != null) builder.accountName(request.getAccountName());
        }
        
        ApproveRefundCommand command = builder.build();
        pipeline.send(command);
        
        return ResponseEntity.ok(ResultRes.successResult(null, "Phê duyệt hoàn tiền thành công!", 200));
    }
}
