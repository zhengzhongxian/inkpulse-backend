package com.inkpulse.features.order.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.OrderDetail;
import com.inkpulse.entities.enums.OrderStatus;
import com.inkpulse.features.order.queries.GetInternalOrdersQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.book.BookEditionResponse;
import com.inkpulse.models.response.order.OrderSummaryResponse;
import com.inkpulse.repositories.OrderDetailRepository;
import com.inkpulse.repositories.OrderRepository;
import com.inkpulse.corehelpers.UrlHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetInternalOrdersQueryHandler implements Query.QueryHandler<GetInternalOrdersQuery, PagedList<OrderSummaryResponse>> {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${minio.use-ssl:false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public PagedList<OrderSummaryResponse> handle(GetInternalOrdersQuery query) {
        log.info("Handling GetInternalOrdersQuery for admin. Page: {}, Size: {}, Search: {}, Status: {}",
                query.getPageNumber(), query.getPageSize(), query.getSearchKeyword(), query.getStatus());

        // Default sorting
        if (query.getSortBy() == null || query.getSortBy().trim().isEmpty()) {
            query.setSortBy("createdAt");
            query.setSortDirection("desc");
        }

        Pageable basePageable = query.toPageable();
        Pageable pageable = PageRequest.of(
                basePageable.getPageNumber(),
                basePageable.getPageSize(),
                basePageable.getSort().and(Sort.by(Sort.Direction.ASC, "id"))
        );

        OrderStatus statusEnum = null;
        if (query.getStatus() != null && !query.getStatus().trim().isEmpty() && !"ALL".equalsIgnoreCase(query.getStatus())) {
            try {
                statusEnum = OrderStatus.valueOf(query.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid order status query parameter: {}", query.getStatus());
            }
        }

        String searchKeyword = query.getSearchKeyword();
        if (searchKeyword != null && searchKeyword.trim().isEmpty()) {
            searchKeyword = null;
        }

        Page<Order> ordersPage;
        if (searchKeyword == null) {
            if (statusEnum == null) {
                ordersPage = orderRepository.findAll(pageable);
            } else {
                ordersPage = orderRepository.findByOrderStatus(statusEnum, pageable);
            }
        } else {
            if (statusEnum == null) {
                ordersPage = orderRepository.searchOrdersInternalNoStatus(searchKeyword, pageable);
            } else {
                ordersPage = orderRepository.searchOrdersInternal(statusEnum, searchKeyword, pageable);
            }
        }

        return PagedList.fromPage(ordersPage, order -> {
            List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
            int totalItems = details.size();

            String firstItemTitle = "";
            String firstItemThumbnail = null;

            if (!details.isEmpty()) {
                OrderDetail firstDetail = details.get(0);
                BookEdition firstEdition = firstDetail.getBookEdition();
                if (firstEdition != null) {
                    firstItemTitle = firstEdition.getBook() != null ? firstEdition.getBook().getTitle() : firstEdition.getIsbn();
                    firstItemThumbnail = UrlHelper.buildAbsoluteUrl(publicUrl, firstEdition.getThumbnailUrl(), useSsl);
                }
            }

            BigDecimal totalAmount = order.getOrderFee().add(order.getShippingFee());

            return new OrderSummaryResponse(
                    order.getId().toString(),
                    order.getOrderCode(),
                    order.getOrderStatus().name(),
                    order.getPaymentMethod().name(),
                    order.getPaymentStatus().name(),
                    BookEditionResponse.formatVnd(order.getOrderFee()),
                    BookEditionResponse.formatVnd(order.getShippingFee()),
                    BookEditionResponse.formatVnd(totalAmount),
                    totalItems,
                    firstItemTitle,
                    firstItemThumbnail,
                    order.getCreatedAt().toString()
            );
        });
    }
}
