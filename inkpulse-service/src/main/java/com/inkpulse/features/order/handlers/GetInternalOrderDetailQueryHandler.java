package com.inkpulse.features.order.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.cqrs.Query;
import com.inkpulse.crypto.ICryptographyService;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.OrderDetail;
import com.inkpulse.models.response.book.BookEditionResponse;
import com.inkpulse.features.order.queries.GetInternalOrderDetailQuery;
import com.inkpulse.models.response.order.OrderDetailResponse;
import com.inkpulse.models.response.order.OrderItemDetailResponse;
import com.inkpulse.repositories.OrderRepository;
import com.inkpulse.repositories.OrderDetailRepository;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetInternalOrderDetailQueryHandler implements Query.QueryHandler<GetInternalOrderDetailQuery, OrderDetailResponse> {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ICryptographyService cryptographyService;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${minio.use-ssl:false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse handle(GetInternalOrderDetailQuery query) {
        log.info("Handling GetInternalOrderDetailQuery for admin. OrderId: {}", query.orderId());

        Order order = orderRepository.findById(query.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", query.orderId()));

        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        List<OrderItemDetailResponse> itemResponses = new ArrayList<>();

        for (OrderDetail detail : details) {
            BookEdition edition = detail.getBookEdition();
            String authorNameJoined = "";
            if (edition.getBook().getBookAuthors() != null) {
                authorNameJoined = edition.getBook().getBookAuthors().stream()
                        .filter(ba -> ba.isActive() && ba.getAuthor() != null)
                        .map(ba -> ba.getAuthor().getName())
                        .collect(Collectors.joining(", "));
            }

            BigDecimal subtotal = detail.getOriginalPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));

            itemResponses.add(new OrderItemDetailResponse(
                    edition.getId().toString(),
                    edition.getBook().getTitle(),
                    authorNameJoined,
                    UrlHelper.buildAbsoluteUrl(publicUrl, edition.getThumbnailUrl(), useSsl),
                    detail.getQuantity(),
                    BookEditionResponse.formatVnd(detail.getOriginalPrice()),
                    BookEditionResponse.formatVnd(subtotal)
            ));
        }

        String fullAddress = order.getStreetAddress() + ", " 
                + order.getWard().getWardName() + ", " 
                + order.getDistrict().getDistrictName() + ", " 
                + order.getProvince().getProvinceName();

        String decryptedPhone = cryptographyService.decrypt(order.getRecipientPhone());

        BigDecimal totalAmount = order.getOrderFee().add(order.getShippingFee());

        return new OrderDetailResponse(
                order.getId().toString(),
                order.getUser().getId().toString(),
                order.getOrderCode(),
                order.getGhnOrderCode(),
                order.getOrderStatus().name(),
                order.getPaymentMethod().name(),
                order.getPaymentStatus().name(),
                order.getReceiverName(),
                decryptedPhone,
                fullAddress,
                order.getAddressLabel(),
                BookEditionResponse.formatVnd(order.getOrderFee()),
                BookEditionResponse.formatVnd(order.getShippingFee()),
                BookEditionResponse.formatVnd(totalAmount),
                itemResponses,
                order.getCreatedAt().toString()
        );
    }
}
