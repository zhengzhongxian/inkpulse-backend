package com.inkpulse.models.response.flashsale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleResponse {
    private String flashSaleId;
    private String name;
    private Integer itemCount;
    private Boolean isActive;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private java.time.LocalDateTime createdAt;
}
