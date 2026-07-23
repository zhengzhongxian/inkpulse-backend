package com.inkpulse.features.order.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.cqrs.Query;
import com.inkpulse.crypto.ICryptographyService;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.OrderDetail;
import com.inkpulse.models.response.book.BookEditionResponse;
import com.inkpulse.features.order.queries.GetOrderDetailQuery;
import com.inkpulse.models.response.order.OrderDetailResponse;
import com.inkpulse.models.response.order.OrderItemDetailResponse;
import com.inkpulse.repositories.OrderRepository;
import com.inkpulse.repositories.OrderDetailRepository;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import com.inkpulse.entities.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetOrderDetailQueryHandler implements Query.QueryHandler<GetOrderDetailQuery, OrderDetailResponse> {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ICryptographyService cryptographyService;
    private final SectionCacheService sectionCache;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${minio.use-ssl:false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse handle(GetOrderDetailQuery query) {
        String orderIdStr = query.orderId().toString();

        // 1. Try to read from cache (Cache-Aside Pattern)
        OrderDetailResponse cachedDetail = sectionCache.get(orderIdStr, OrderDetailResponse.class);
        if (cachedDetail != null) {
            if (!cachedDetail.userId().equals(query.userId().toString())) {
                log.warn("User {} unauthorized to view order {}", query.userId(), query.orderId());
                throw new BusinessValidationException("Bạn không có quyền xem đơn hàng này!", "UNAUTHORIZED");
            }
            log.debug("Cache hit for order detail: {}", orderIdStr);
            return cachedDetail;
        }

        log.debug("Cache miss for order detail: {}. Fetching from DB...", orderIdStr);

        // 2. Fetch from DB
        Order order = orderRepository.findByIdAndUserId(query.orderId(), query.userId())
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
            BigDecimal oldPrice = edition.getOldPrice() != null ? edition.getOldPrice() : detail.getOriginalPrice();
            String oldPriceDisplay = BookEditionResponse.formatVnd(oldPrice);
            String voucherDiscountAmountDisplay = BookEditionResponse.formatVnd(detail.getVoucherDiscountAmount() != null ? detail.getVoucherDiscountAmount() : BigDecimal.ZERO);
            Boolean isVoucherApplied = detail.getVoucherDiscountAmount() != null && detail.getVoucherDiscountAmount().compareTo(BigDecimal.ZERO) > 0;

            itemResponses.add(new OrderItemDetailResponse(
                    edition.getId().toString(),
                    edition.getBook().getTitle(),
                    authorNameJoined,
                    UrlHelper.buildAbsoluteUrl(publicUrl, edition.getThumbnailUrl(), useSsl),
                    detail.getQuantity(),
                    BookEditionResponse.formatVnd(detail.getOriginalPrice()),
                    BookEditionResponse.formatVnd(subtotal),
                    edition.getEditionNumber(),
                    edition.getCoverType() != null ? edition.getCoverType().name() : null,
                    edition.getIsbn(),
                    detail.getFlashSale() != null,
                    BookEditionResponse.formatVnd(detail.getFlashSaleDiscountAmount()),
                    detail.getFlashSale() != null ? 
                        detail.getFlashSale().getItems().stream()
                            .filter(i -> i.getBookEdition().getId().equals(edition.getId()))
                            .findFirst()
                            .map(i -> i.getId().toString())
                            .orElse(detail.getFlashSale().getId().toString())
                        : null,
                    oldPriceDisplay,
                    voucherDiscountAmountDisplay,
                    isVoucherApplied
            ));
        }

        String fullAddress = order.getStreetAddress() + ", " 
                + order.getWard().getWardName() + ", " 
                + order.getDistrict().getDistrictName() + ", " 
                + order.getProvince().getProvinceName();

        String decryptedPhone = cryptographyService.decrypt(order.getRecipientPhone());

        BigDecimal totalAmount = order.getOrderFee().add(order.getShippingFee())
                .subtract(order.getVoucherDiscountAmount() != null ? order.getVoucherDiscountAmount() : BigDecimal.ZERO);

        OrderDetailResponse detailResponse = new OrderDetailResponse(
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
                order.getCreatedAt().toString(),
                order.getVoucher() != null ? order.getVoucher().getVoucherCode() : null,
                order.getVoucherDiscountAmount() != null ? BookEditionResponse.formatVnd(order.getVoucherDiscountAmount()) : BookEditionResponse.formatVnd(BigDecimal.ZERO)
        );

        // 3. Save to Cache (Only cache if order is in a final status: DELIVERED or CANCELLED)
        if (order.getOrderStatus() == OrderStatus.DELIVERED || order.getOrderStatus() == OrderStatus.CANCELLED) {
            sectionCache.set(detailResponse);
            log.debug("Saved order detail to cache for order: {}", orderIdStr);
        }

        return detailResponse;
    }
}
