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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private final IGhnShippingService ghnShippingService;
    private final IPayOsService payOsService;
    private final PayOsSettings payOsSettings;
    private final ICryptographyService cryptographyService;
    private final OutboxPublisher outboxPublisher;

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

        // 5. Generate unique numeric order code (for PayOS compatibility)
        String orderCode = OrderCodeHelper.generateOrderCode();
        while (orderRepository.existsByOrderCode(orderCode)) {
            orderCode = OrderCodeHelper.generateOrderCode();
        }

        PaymentMethod paymentMethod = PaymentMethod.valueOf(command.getPaymentMethod().trim().toUpperCase());
        OrderStatus initialStatus = paymentMethod == PaymentMethod.PAYOS ? OrderStatus.PENDING_PAYMENT : OrderStatus.PENDING;

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
                .orderFee(totalInsuranceValue)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        order = orderRepository.saveAndFlush(order);

        // 7. Save Order Details
        for (OrderItemRequest item : command.getItems()) {
            BookEdition edition = pipelineCtx.getEditions().get(item.getEditionId());
            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .bookEdition(edition)
                    .quantity(item.getQuantity())
                    .originalPrice(edition.getPrice())
                    .flashSaleDiscountAmount(BigDecimal.ZERO)
                    .voucherDiscountAmount(BigDecimal.ZERO)
                    .build();
            orderDetailRepository.save(detail);
        }

        // 8. Create Payment Transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .transactionCode(orderCode)
                .amount(totalInsuranceValue.add(shippingFee))
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

        // 10. Deduct Stock Quantity
        for (OrderItemRequest item : command.getItems()) {
            BookEdition edition = pipelineCtx.getEditions().get(item.getEditionId());
            edition.setStockQuantity(edition.getStockQuantity() - item.getQuantity());
            bookEditionRepository.save(edition);
        }

        // 11. Clear Cart Items (if checked out from cart)
        if (command.getSource() != null && command.getSource().equalsIgnoreCase("CART")
                && command.getCartItemIds() != null && !command.getCartItemIds().isEmpty()) {
            cartItemRepository.deleteAllByIdInAndCart_User_Id(command.getCartItemIds(), user.getId());
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
                    .amount(totalInsuranceValue.add(shippingFee).longValue())
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
