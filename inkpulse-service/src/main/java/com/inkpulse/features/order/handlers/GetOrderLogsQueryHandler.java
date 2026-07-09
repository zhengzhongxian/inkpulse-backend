package com.inkpulse.features.order.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.OrderLog;
import com.inkpulse.features.order.queries.GetOrderLogsQuery;
import com.inkpulse.models.response.order.OrderLogResponse;
import com.inkpulse.repositories.OrderLogRepository;
import com.inkpulse.repositories.OrderRepository;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetOrderLogsQueryHandler implements Query.QueryHandler<GetOrderLogsQuery, List<OrderLogResponse>> {

    private final OrderLogRepository orderLogRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderLogResponse> handle(GetOrderLogsQuery query) {
        log.info("Handling GetOrderLogsQuery for orderCode: {}", query.orderCode());

        if (!orderRepository.existsByOrderCode(query.orderCode())) {
            throw new ResourceNotFoundException("Order", "orderCode", query.orderCode());
        }

        List<OrderLog> logs = orderLogRepository.findByOrderOrderCodeOrderByCreatedAtDesc(query.orderCode());

        return logs.stream().map(logItem -> new OrderLogResponse(
                logItem.getId().toString(),
                logItem.getFromStatus().name(),
                logItem.getToStatus().name(),
                logItem.getChangedBy() != null ? logItem.getChangedBy().toString() : "SYSTEM",
                logItem.getAdminNote(),
                logItem.getUserNote(),
                logItem.getCreatedAt().toString()
        )).collect(Collectors.toList());
    }
}
