package com.inkpulse.features.order.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.OrderDetail;
import com.inkpulse.models.response.book.BookEditionResponse;
import com.inkpulse.features.order.queries.GetMyOrdersQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.order.OrderSummaryResponse;
import com.inkpulse.repositories.OrderRepository;
import com.inkpulse.repositories.OrderDetailRepository;
import com.inkpulse.corehelpers.UrlHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetMyOrdersQueryHandler implements Query.QueryHandler<GetMyOrdersQuery, PagedList<OrderSummaryResponse>> {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${minio.use-ssl:false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public PagedList<OrderSummaryResponse> handle(GetMyOrdersQuery query) {
        log.info("Handling GetMyOrdersQuery for user: {}", query.getUserId());
        Pageable pageable = query.toPageable();

        Page<Order> ordersPage = orderRepository.findByUserId(query.getUserId(), pageable);

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

            BigDecimal discount = order.getVoucherDiscountAmount() != null ? order.getVoucherDiscountAmount() : BigDecimal.ZERO;
            BigDecimal totalAmount = order.getOrderFee().add(order.getShippingFee()).subtract(discount);
            if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                totalAmount = BigDecimal.ZERO;
            }

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
