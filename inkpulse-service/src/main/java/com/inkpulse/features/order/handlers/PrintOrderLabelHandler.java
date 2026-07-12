package com.inkpulse.features.order.handlers;

import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Order;
import com.inkpulse.features.order.commands.PrintOrderLabelCommand;
import com.inkpulse.models.response.order.PrintOrderLabelResponse;
import com.inkpulse.repositories.OrderRepository;
import com.inkpulse.service.ghn.IGhnShippingService;
import com.inkpulse.service.ghn.GhnSettings;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrintOrderLabelHandler implements Command.CommandHandler<PrintOrderLabelCommand, PrintOrderLabelResponse> {

    private final OrderRepository orderRepository;
    private final IGhnShippingService ghnShippingService;
    private final GhnSettings ghnSettings;

    @Override
    @Transactional(readOnly = true)
    public PrintOrderLabelResponse handle(PrintOrderLabelCommand command) {
        log.info("Handling PrintOrderLabelCommand for orderCode: {} by admin: {}", command.getOrderCode(), command.getAdminUserId());

        Order order = orderRepository.findByOrderCode(command.getOrderCode())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderCode", command.getOrderCode()));

        String ghnOrderCode = order.getGhnOrderCode();
        if (ghnOrderCode == null || ghnOrderCode.trim().isEmpty()) {
            throw new BusinessValidationException("Đơn hàng chưa có mã vận đơn GHN! Vui lòng xác nhận đóng gói trước.", "MISSING_GHN_ORDER_CODE");
        }

        // Call GHN to generate token
        String token = ghnShippingService.generatePrintToken(ghnOrderCode);

        // Construct print URL
        String printUrl = ghnSettings.getBaseUrl() + "/a5/public-api/printA5?token=" + token;
        log.info("Generated print URL for order {}: {}", command.getOrderCode(), printUrl);

        return new PrintOrderLabelResponse(printUrl);
    }
}
