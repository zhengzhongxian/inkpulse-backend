package com.inkpulse.models.response.flashsale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleItemResponse {
    private String flashSaleItemId;
    private String flashSaleId;
    private String name; // campaign name
    private String bookEditionId;
    private String bookTitle;
    private String editionTitle; // isbn
    private String thumbnailUrl;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal flashSalePrice;
    private Integer flashSaleStock;
    private Integer soldCount;
    private ZonedDateTime startDate; // campaign dates copied for frontend
    private ZonedDateTime endDate;
}
