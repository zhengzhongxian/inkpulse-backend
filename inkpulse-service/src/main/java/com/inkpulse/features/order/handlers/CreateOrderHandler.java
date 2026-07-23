package com.inkpulse.features.order.handlers;

import com.inkpulse.constants.QueueConstants;
import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.corehelpers.OrderCodeHelper;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.crypto.ICryptographyService;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.CoverType;
import com.inkpulse.entities.enums.OrderStatus;
import com.inkpulse.entities.enums.PaymentMethod;
import com.inkpulse.entities.enums.PaymentStatus;
import com.inkpulse.features.order.commands.CreateOrderCommand;
import com.inkpulse.models.response.order.CreateOrderResponse;
import com.inkpulse.models.request.order.OrderItemRequest;
import com.inkpulse.features.order.rules.CreateOrderContext;
import com.inkpulse.pipeline.EligibilityPipeline;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.*;
import com.inkpulse.service.ghn.IGhnShippingService;
import com.inkpulse.models.request.ghn.GhnCalculateFeeRequest;
import com.inkpulse.models.response.ghn.GhnCalculateFeeResponse;
import com.inkpulse.models.request.ghn.GhnShippingItem;
import com.inkpulse.service.payos.IPayOsService;
import com.inkpulse.service.payos.PayOsSettings;
import com.inkpulse.service.outbox.OutboxPublisher;
import com.inkpulse.entities.OrderEvent;
import com.inkpulse.entities.StockTransaction;
import com.inkpulse.entities.enums.OrderEventType;
import com.inkpulse.entities.enums.StockTransactionType;
import com.inkpulse.repositories.OrderEventRepository;
import com.inkpulse.repositories.StockTransactionRepository;
import com.inkpulse.features.book.dto.BookEditionSyncHelper;
import com.inkpulse.features.book.dto.SyncBookEditionMessage;
import com.inkpulse.constants.message.StockMessageConstants;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.cache.CacheProperties;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.corehelpers.JsonHelper;
import com.inkpulse.entities.enums.UserVoucherStatus;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.features.voucher.strategies.VoucherTargetStrategy;
import com.inkpulse.features.voucher.strategies.VoucherTargetStrategyResolver;
import com.inkpulse.features.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderHandler implements Command.CommandHandler<CreateOrderCommand, CreateOrderResponse> {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderLogRepository orderLogRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserAddressRepository userAddressRepository;
    private final BookEditionRepository bookEditionRepository;
    private final GhnProvinceRepository ghnProvinceRepository;
    private final GhnDistrictRepository ghnDistrictRepository;
    private final GhnWardRepository ghnWardRepository;
    private final CartItemRepository cartItemRepository;
    private final UserVoucherRepository userVoucherRepository;

    private final IGhnShippingService ghnShippingService;
    private final IPayOsService payOsService;
    private final PayOsSettings payOsSettings;
    private final ICryptographyService cryptographyService;
    private final OutboxPublisher outboxPublisher;

    private final OrderEventRepository orderEventRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final BookEditionSyncHelper bookEditionSyncHelper;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;
    private final VoucherTargetStrategyResolver strategyResolver;
    private final TokenService tokenService;

    private final List<IEligibilityRule<CreateOrderContext>> orderRules;

    @Override
    @Transactional
    public CreateOrderResponse handle(CreateOrderCommand command) {
        log.info("Handling CreateOrderCommand for user: {}", command.getUserId());

        // 1. Load User
        User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new BusinessValidationException("Không tìm thấy user", "USER_NOT_FOUND"));

        // 2. Run Eligibility Pipeline
        CreateOrderContext pipelineCtx = new CreateOrderContext(command);
        pipelineCtx.setUser(user);

        EligibilityPipeline<CreateOrderContext> pipeline = new EligibilityPipeline<>(orderRules);
        var resultContext = pipeline.run(pipelineCtx);

        if (resultContext.isRejected()) {
            log.warn("Order placement rejected: {}", resultContext.getRejectionReason());
            throw new BusinessValidationException(resultContext.getRejectionReason(), "ORDER_REJECTED");
        }

        // Register transaction hook for Flash Sale Redis state rollback/commit
        boolean hasFlashSale = command.getItems().stream().anyMatch(item -> item.getFlashSaleItemId() != null);
        if (hasFlashSale) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK) {
                            log.info("Order transaction rolled back. Reverting Redis stock decrements...");
                            for (OrderItemRequest item : command.getItems()) {
                                if (item.getFlashSaleItemId() != null) {
                                    try {
                                        cacheService.hashIncrement(KeyConstants.SECTION_FLASHSALE_STOCK, item.getFlashSaleItemId().toString(), item.getQuantity());
                                        log.info("Reverted Redis stock for flash sale item {} (+{})", item.getFlashSaleItemId(), item.getQuantity());
                                    } catch (Exception e) {
                                        log.error("Failed to revert Redis stock for flash sale item {}", item.getFlashSaleItemId(), e);
                                    }
                                }
                            }
                        } else if (status == org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED) {
                            log.info("Order transaction committed. Adding buyers to Redis set...");
                            for (OrderItemRequest item : command.getItems()) {
                                if (item.getFlashSaleItemId() != null) {
                                    try {
                                        String buyersKey = KeyConstants.SECTION_FLASHSALE_BUYERS + ":" + item.getFlashSaleItemId();
                                        com.inkpulse.entities.FlashSaleItem flashSaleItem = pipelineCtx.getActiveFlashSaleItems().get(item.getEditionId());
                                        java.time.Duration ttl = java.time.Duration.ofHours(24);
                                        if (flashSaleItem != null && flashSaleItem.getFlashSale() != null) {
                                            long secondsLeft = java.time.temporal.ChronoUnit.SECONDS.between(ZonedDateTime.now(), flashSaleItem.getFlashSale().getEndDate());
                                            if (secondsLeft > 0) {
                                                ttl = java.time.Duration.ofSeconds(secondsLeft);
                                            }
                                        }
                                        cacheService.sadd(buyersKey, ttl, user.getId().toString());
                                        log.info("Added user {} to buyers set of flash sale item {}", user.getId(), item.getFlashSaleItemId());
                                    } catch (Exception e) {
                                        log.error("Failed to add user to buyers set of flash sale item {}", item.getFlashSaleItemId(), e);
                                    }
                                }
                            }
                        }
                    }
                }
            );
        }

        // 3. Resolve Address
        UserAddress address = pipelineCtx.getAddress();
        GhnProvince province;
        GhnDistrict district;
        GhnWard ward;
        String streetAddress;
        String recipientPhone;
        String receiverName;
        String addressLabel;

        if (address != null) {
            province = address.getProvince();
            district = address.getDistrict();
            ward = address.getWard();
            streetAddress = address.getStreetAddress();
            recipientPhone = address.getRecipientPhone(); // This is decrypted by converter when loaded
            receiverName = command.getReceiverName() != null && !command.getReceiverName().trim().isEmpty()
                    ? command.getReceiverName().trim()
                    : (user.getProfile() != null ? user.getProfile().getFullName() : user.getUsername());
            addressLabel = address.getAddressLabel() != null ? address.getAddressLabel() : "Home";

            // Update lastUsedAt
            address.setLastUsedAt(LocalDateTime.now());
            userAddressRepository.save(address);
        } else {
            // Retrieve entities for new address
            province = ghnProvinceRepository.findById(command.getProvinceId()).orElseThrow();
            district = ghnDistrictRepository.findById(command.getDistrictId()).orElseThrow();
            ward = ghnWardRepository.findById(command.getWardCode()).orElseThrow();
            streetAddress = command.getStreetAddress().trim();
            recipientPhone = command.getRecipientPhone().trim();
            receiverName = command.getReceiverName().trim();
            addressLabel = command.getAddressLabel() != null && !command.getAddressLabel().trim().isEmpty()
                    ? command.getAddressLabel().trim()
                    : "Home";

            // Save new address for user
            address = UserAddress.builder()
                    .user(user)
                    .province(province)
                    .district(district)
                    .ward(ward)
                    .streetAddress(streetAddress)
                    .recipientPhone(recipientPhone) // Plain string: Converter will encrypt automatically on save
                    .addressLabel(addressLabel)
                    .lastUsedAt(LocalDateTime.now())
                    .build();
            address = userAddressRepository.save(address);
        }

        // 4. Re-calculate Shipping Fee on Server Side (Anti-fraud)
        int totalWeight = 0;
        int totalHeight = 0;
        int maxWidth = 0;
        int maxLength = 0;
        BigDecimal totalInsuranceValue = BigDecimal.ZERO;
        List<GhnShippingItem> ghnItems = new ArrayList<>();

        for (OrderItemRequest item : command.getItems()) {
            BookEdition edition = pipelineCtx.getEditions().get(item.getEditionId());
            int qty = item.getQuantity();

            totalWeight += edition.getWeightGram() * qty;
            totalHeight += edition.getHeightCm() * qty;
            maxWidth = Math.max(maxWidth, edition.getWidthCm());
            maxLength = Math.max(maxLength, edition.getLengthCm());

            BigDecimal itemPrice = edition.getPrice().multiply(BigDecimal.valueOf(qty));
            totalInsuranceValue = totalInsuranceValue.add(itemPrice);

            ghnItems.add(GhnShippingItem.builder()
                    .name(edition.getBook() != null ? edition.getBook().getTitle() : edition.getIsbn())
                    .quantity(qty)
                    .weight(edition.getWeightGram())
                    .length(edition.getLengthCm())
                    .width(edition.getWidthCm())
                    .height(edition.getHeightCm())
                    .build());
        }

        int insuranceVal = Math.min(5000000, totalInsuranceValue.intValue());

        GhnCalculateFeeRequest ghnRequest = GhnCalculateFeeRequest.builder()
                .toDistrictId(district.getDistrictId())
                .toWardCode(ward.getWardCode())
                .weight(totalWeight)
                .length(maxLength)
                .width(maxWidth)
                .height(totalHeight)
                .insuranceValue(insuranceVal)
                .items(ghnItems)
                .build();

        GhnCalculateFeeResponse ghnResponse = ghnShippingService.calculateShippingFee(ghnRequest);
        BigDecimal shippingFee = ghnResponse.getData().getTotal();

        // Voucher Discount Calculations
        Voucher voucher = pipelineCtx.getAppliedVoucher();
        BigDecimal totalVoucherDiscount = BigDecimal.ZERO;
        Map<UUID, BigDecimal> itemVoucherDiscounts = new HashMap<>();

        if (voucher != null) {
            if (voucher.getTargetType() == VoucherTargetType.SHIPPING) {
                if (voucher.getDiscountType() == com.inkpulse.entities.enums.VoucherDiscountType.PERCENTAGE) {
                    BigDecimal calculated = shippingFee.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    if (voucher.getMaxDiscountAmount() != null) {
                        calculated = calculated.min(voucher.getMaxDiscountAmount());
                    }
                    totalVoucherDiscount = calculated.min(shippingFee);
                } else {
                    totalVoucherDiscount = voucher.getDiscountValue().min(shippingFee);
                }
            } else {
                VoucherTargetStrategy strategy = strategyResolver.resolve(voucher.getTargetType());
                List<OrderItemRequest> eligibleItems = new ArrayList<>();
                BigDecimal eligibleItemsSubtotal = BigDecimal.ZERO;

                for (OrderItemRequest item : command.getItems()) {
                    BookEdition edition = pipelineCtx.getEditions().get(item.getEditionId());
                    if (edition != null && strategy.isEligible(voucher, edition)) {
                        eligibleItems.add(item);
                        BigDecimal itemPrice = edition.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                        eligibleItemsSubtotal = eligibleItemsSubtotal.add(itemPrice);
                    }
                }

                if (!eligibleItems.isEmpty() && eligibleItemsSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal allowedDiscount;
                    if (voucher.getDiscountType() == com.inkpulse.entities.enums.VoucherDiscountType.PERCENTAGE) {
                        BigDecimal calculated = eligibleItemsSubtotal.multiply(voucher.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        if (voucher.getMaxDiscountAmount() != null) {
                            calculated = calculated.min(voucher.getMaxDiscountAmount());
                        }
                        allowedDiscount = calculated.min(eligibleItemsSubtotal);
                    } else {
                        allowedDiscount = voucher.getDiscountValue().min(eligibleItemsSubtotal);
                    }

                    totalVoucherDiscount = allowedDiscount;

                    BigDecimal distributedSum = BigDecimal.ZERO;
                    for (int i = 0; i < eligibleItems.size(); i++) {
                        OrderItemRequest item = eligibleItems.get(i);
                        BookEdition edition = pipelineCtx.getEditions().get(item.getEditionId());
                        BigDecimal itemPrice = edition.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

                        BigDecimal itemDiscount;
                        if (i == eligibleItems.size() - 1) {
                            itemDiscount = allowedDiscount.subtract(distributedSum);
                        } else {
                            itemDiscount = allowedDiscount.multiply(itemPrice).divide(eligibleItemsSubtotal, 2, RoundingMode.HALF_UP);
                            distributedSum = distributedSum.add(itemDiscount);
                        }
                        itemVoucherDiscounts.put(item.getEditionId(), itemDiscount);
                    }
                }
            }
        }

        // 5. Generate unique numeric order code (for PayOS compatibility)
        String orderCode = OrderCodeHelper.generateOrderCode();
        while (orderRepository.existsByOrderCode(orderCode)) {
            orderCode = OrderCodeHelper.generateOrderCode();
        }

        PaymentMethod paymentMethod = PaymentMethod.valueOf(command.getPaymentMethod().trim().toUpperCase());
        OrderStatus initialStatus = paymentMethod == PaymentMethod.PAYOS ? OrderStatus.PENDING_PAYMENT : OrderStatus.PENDING;

        // Calculate Flash Sale discounts
        BigDecimal totalFlashSaleDiscount = BigDecimal.ZERO;
        for (OrderItemRequest item : command.getItems()) {
            com.inkpulse.entities.FlashSaleItem flashSaleItem = pipelineCtx.getActiveFlashSaleItems().get(item.getEditionId());
            if (flashSaleItem != null) {
                totalFlashSaleDiscount = totalFlashSaleDiscount.add(flashSaleItem.getDiscountAmount().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        BigDecimal finalProductSubtotal = totalInsuranceValue.subtract(totalFlashSaleDiscount);

        // 6. Build and Save Order (Hibernate automatically encrypts plain recipientPhone on persist)
        Order order = Order.builder()
                .user(user)
                .province(province)
                .district(district)
                .ward(ward)
                .recipientPhone(recipientPhone) // Plain text, auto-encrypted by Converter
                .receiverName(receiverName)
                .orderCode(orderCode)
                .streetAddress(streetAddress)
                .orderStatus(initialStatus)
                .addressLabel(addressLabel)
                .shippingFee(shippingFee)
                .orderFee(finalProductSubtotal)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PENDING)
                .voucher(voucher)
                .voucherDiscountAmount(totalVoucherDiscount)
                .build();

        order = orderRepository.saveAndFlush(order);

        // Save Order Event (Order Event Store)
        OrderEvent orderEvent = OrderEvent.builder()
                .order(order)
                .eventType(OrderEventType.ORDER_CREATED)
                .eventData(JsonHelper.serializeSafe(command))
                .createdBy(user.getId())
                .build();
        orderEventRepository.save(orderEvent);

        // 7. Save Order Details
        for (OrderItemRequest item : command.getItems()) {
            BookEdition edition = pipelineCtx.getEditions().get(item.getEditionId());
            BigDecimal itemDiscount = itemVoucherDiscounts.getOrDefault(item.getEditionId(), BigDecimal.ZERO);
            BigDecimal flashDiscount = pipelineCtx.getItemFlashSaleDiscounts().getOrDefault(item.getEditionId(), BigDecimal.ZERO);
            com.inkpulse.entities.FlashSaleItem flashSaleItem = pipelineCtx.getActiveFlashSaleItems().get(item.getEditionId());

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .bookEdition(edition)
                    .quantity(item.getQuantity())
                    .originalPrice(edition.getPrice())
                    .flashSaleDiscountAmount(flashDiscount.multiply(BigDecimal.valueOf(item.getQuantity())))
                    .voucherDiscountAmount(itemDiscount)
                    .voucher(itemDiscount.compareTo(BigDecimal.ZERO) > 0 ? voucher : null)
                    .flashSale(flashSaleItem != null ? flashSaleItem.getFlashSale() : null)
                    .build();
            orderDetailRepository.save(detail);
        }

        // Update UserVoucher link status to USED
        if (voucher != null && pipelineCtx.getUserVoucherLink() != null) {
            UserVoucher uv = pipelineCtx.getUserVoucherLink();
            uv.setStatus(UserVoucherStatus.USED);
            uv.setOrder(order);
            uv.setUsedAt(ZonedDateTime.now());
            userVoucherRepository.save(uv);
        }

        // 8. Create Payment Transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .transactionCode(orderCode)
                .amount(finalProductSubtotal.add(shippingFee).subtract(totalVoucherDiscount))
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING)
                .build();
        transaction = paymentTransactionRepository.save(transaction);

        // 9. Create Order Log
        OrderLog orderLog = OrderLog.builder()
                .order(order)
                .fromStatus(initialStatus)
                .toStatus(initialStatus)
                .changedBy(user.getId())
                .adminNote("Khởi tạo đơn hàng")
                .userNote("Khởi tạo đơn hàng")
                .build();
        orderLogRepository.save(orderLog);

        // 10. Deduct Stock Quantity (Atomic SQL + Stock Event + ES/Redis Sync)
        for (OrderItemRequest item : command.getItems()) {
            BookEdition edition = pipelineCtx.getEditions().get(item.getEditionId());
            int affected = bookEditionRepository.decrementStock(edition.getId(), item.getQuantity());
            if (affected == 0) {
                throw new BusinessValidationException(
                        String.format(StockMessageConstants.STOCK_INSUFFICIENT, 
                                edition.getBook() != null ? edition.getBook().getTitle() : edition.getIsbn(), 
                                edition.getStockQuantity()), 
                        StockMessageConstants.CODE_STOCK_INSUFFICIENT
                );
            }

            // Update local object to sync with ELS correctly
            edition.setStockQuantity(edition.getStockQuantity() - item.getQuantity());

            // Save Stock Transaction
            StockTransaction tx = StockTransaction.builder()
                    .edition(edition)
                    .delta(-item.getQuantity())
                    .type(StockTransactionType.EXPORT_ORDER)
                    .referenceCode(orderCode)
                    .note(OrderMessageConstants.CREATE_ORDER_SUCCESS)
                    .createdBy(user.getId())
                    .build();
            stockTransactionRepository.save(tx);

            // Sync with Elasticsearch
            SyncBookEditionMessage syncMsg = bookEditionSyncHelper.buildSyncMessage(edition);
            if (syncMsg != null) {
                outboxPublisher.publish(
                        QueueConstants.SYNC_BOOK_EDITION_PARTIAL,
                        syncMsg,
                        "urn:message:InkPulse.Worker.Features.Book.Messages:SyncBookEditionMessage"
                );
            }

            // Evict Cache
            try {
                String cacheKey = cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL, edition.getId().toString());
                cacheService.remove(cacheKey);
            } catch (Exception ex) {
                log.error("Failed to evict detail cache for edition: {}", edition.getId(), ex);
            }
        }

        // 11. Clear Cart Items (if checked out from cart)
        if (command.getSource() != null && command.getSource().equalsIgnoreCase("CART")
                && command.getCartItemIds() != null && !command.getCartItemIds().isEmpty()) {
            cartItemRepository.deleteAllByIdInAndCart_User_Id(command.getCartItemIds(), user.getId());
            tokenService.cacheUserCart(user.getId());
        }

        String checkoutUrl = null;
        String qrCode = null;
        Long expiredAt = null;

        if (paymentMethod == PaymentMethod.PAYOS) {
            // 12. Create PayOS Payment Link
            long expiryTimestamp = (System.currentTimeMillis() / 1000) + (payOsSettings.getExpiryMinutes() * 60L);
            expiredAt = expiryTimestamp;
            long payOsOrderCode = Long.parseLong(orderCode);

            // Alphanumeric, no accents, max 25 chars for PayOS description
            String description = "GD" + orderCode;
            if (description.length() > 25) {
                description = description.substring(0, 25);
            }

            CreatePaymentLinkRequest payOsReq = CreatePaymentLinkRequest.builder()
                    .orderCode(payOsOrderCode)
                    .amount(finalProductSubtotal.add(shippingFee).subtract(totalVoucherDiscount).longValue())
                    .description(description)
                    .returnUrl(payOsSettings.getReturnUrl() + "?status=success&orderCode=" + orderCode)
                    .cancelUrl(payOsSettings.getCancelUrl() + "?status=cancel&orderCode=" + orderCode)
                    .buyerName(receiverName)
                    .buyerEmail(user.getEmail() != null ? cryptographyService.decrypt(user.getEmail()) : "") // Decrypt user email for PayOS if encrypted in DB
                    .buyerPhone(recipientPhone)
                    .expiredAt(expiryTimestamp)
                    .build();

            try {
                CreatePaymentLinkResponse payOsRes = payOsService.createPaymentLink(payOsReq);
                checkoutUrl = payOsRes.getCheckoutUrl();
                qrCode = payOsRes.getQrCode();
                transaction.setTransactionCode(payOsRes.getPaymentLinkId());
                paymentTransactionRepository.save(transaction);
            } catch (Exception e) {
                log.error("Failed to create PayOS payment link for order: {}", orderCode, e);
                throw new BusinessValidationException(OrderMessageConstants.PAYOS_CREATE_LINK_FAILED, "PAYOS_ERROR");
            }
        }

        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .orderCode(orderCode)
                .orderStatus(initialStatus.name())
                .paymentStatus(PaymentStatus.PENDING.name())
                .checkoutUrl(checkoutUrl)
                .qrCode(qrCode)
                .expiredAt(expiredAt)
                .message(OrderMessageConstants.CREATE_ORDER_SUCCESS)
                .build();
    }
}
