package com.inkpulse.features.refund.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.RefundRequest;
import com.inkpulse.entities.enums.RefundStatus;
import com.inkpulse.features.refund.queries.GetRefundRequestsQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.refund.RefundRequestResponse;
import com.inkpulse.repositories.RefundRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetRefundRequestsQueryHandler implements Query.QueryHandler<GetRefundRequestsQuery, PagedList<RefundRequestResponse>> {

    private final RefundRequestRepository refundRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedList<RefundRequestResponse> handle(GetRefundRequestsQuery query) {
        log.info("Handling GetRefundRequestsQuery - page: {}, size: {}, status: {}", 
                query.getPageNumber(), query.getPageSize(), query.getStatus());

        // Build sorting (default: createdAt DESC)
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (query.getSortBy() != null && !query.getSortBy().trim().isEmpty()) {
            Sort.Direction direction = Sort.Direction.DESC;
            if (query.getSortDirection() != null && query.getSortDirection().equalsIgnoreCase("ASC")) {
                direction = Sort.Direction.ASC;
            }
            sort = Sort.by(direction, query.getSortBy());
        }

        // Spring PageRequest is 0-indexed, while PagedRequest is 1-indexed.
        Pageable pageable = PageRequest.of(query.getPageNumber() - 1, query.getPageSize(), sort);

        RefundStatus statusEnum = null;
        if (query.getStatus() != null && !query.getStatus().trim().isEmpty()) {
            try {
                statusEnum = RefundStatus.valueOf(query.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid RefundStatus passed in query: {}", query.getStatus());
            }
        }

        java.time.LocalDateTime startDateTime = null;
        if (query.getStartDate() != null && !query.getStartDate().trim().isEmpty()) {
            try {
                startDateTime = java.time.LocalDate.parse(query.getStartDate().trim()).atStartOfDay();
            } catch (Exception e) {
                log.warn("Failed to parse startDate: {}", query.getStartDate(), e);
            }
        }
        java.time.LocalDateTime endDateTime = null;
        if (query.getEndDate() != null && !query.getEndDate().trim().isEmpty()) {
            try {
                endDateTime = java.time.LocalDate.parse(query.getEndDate().trim()).atTime(23, 59, 59);
            } catch (Exception e) {
                log.warn("Failed to parse endDate: {}", query.getEndDate(), e);
            }
        }

        Page<RefundRequest> page = refundRequestRepository.searchRefunds(statusEnum, query.getSearchKeyword(), startDateTime, endDateTime, pageable);

        return PagedList.fromPage(page, entity -> RefundRequestResponse.builder()
                .id(entity.getId().toString())
                .orderId(entity.getOrder().getId().toString())
                .orderCode(entity.getOrder().getOrderCode())
                .amount(entity.getAmount())
                .status(entity.getStatus().name())
                .reason(entity.getReason())
                .approvedByUsername(entity.getApprovedBy() != null ? entity.getApprovedBy().getUsername() : null)
                .payosRefundId(entity.getPayosRefundId())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build()
        );
    }
}
