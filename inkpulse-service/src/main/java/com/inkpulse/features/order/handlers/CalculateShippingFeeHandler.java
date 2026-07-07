package com.inkpulse.features.order.handlers;

import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.features.order.commands.CalculateShippingFeeCommand;
import com.inkpulse.models.response.order.CalculateShippingFeeResponse;
import com.inkpulse.models.request.order.OrderItemRequest;
import com.inkpulse.repositories.BookEditionRepository;
import com.inkpulse.service.ghn.IGhnShippingService;
import com.inkpulse.models.request.ghn.GhnCalculateFeeRequest;
import com.inkpulse.models.response.ghn.GhnCalculateFeeResponse;
import com.inkpulse.models.request.ghn.GhnShippingItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalculateShippingFeeHandler implements Command.CommandHandler<CalculateShippingFeeCommand, CalculateShippingFeeResponse> {

    private final BookEditionRepository bookEditionRepository;
    private final IGhnShippingService ghnShippingService;

    @Override
    @Transactional(readOnly = true)
    public CalculateShippingFeeResponse handle(CalculateShippingFeeCommand command) {
        log.info("Handling CalculateShippingFeeCommand for user: {}", command.getUserId());

        int totalWeight = 0;
        int totalHeight = 0;
        int maxWidth = 0;
        int maxLength = 0;
        BigDecimal totalInsuranceValue = BigDecimal.ZERO;
        List<GhnShippingItem> ghnItems = new ArrayList<>();

        for (OrderItemRequest item : command.getItems()) {
            Optional<BookEdition> editionOpt = bookEditionRepository.findById(item.getEditionId());
            if (editionOpt.isEmpty()) {
                throw new RuntimeException("Không tìm thấy phiên bản sách!");
            }

            BookEdition edition = editionOpt.get();
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

        // Insurance value for compensation capped at 5.000.000 VND
        int insuranceVal = Math.min(5000000, totalInsuranceValue.intValue());

        GhnCalculateFeeRequest ghnRequest = GhnCalculateFeeRequest.builder()
                .toDistrictId(command.getToDistrictId())
                .toWardCode(command.getToWardCode())
                .weight(totalWeight)
                .length(maxLength)
                .width(maxWidth)
                .height(totalHeight)
                .insuranceValue(insuranceVal)
                .items(ghnItems)
                .build();

        GhnCalculateFeeResponse ghnResponse = ghnShippingService.calculateShippingFee(ghnRequest);
        GhnCalculateFeeResponse.FeeData feeData = ghnResponse.getData();

        return CalculateShippingFeeResponse.builder()
                .total(feeData.getTotal())
                .serviceFee(feeData.getServiceFee())
                .insuranceFee(feeData.getInsuranceFee())
                .couponValue(feeData.getCouponValue())
                .codFailedFee(feeData.getCodFailedFee())
                .build();
    }
}
