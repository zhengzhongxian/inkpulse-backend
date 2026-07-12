package com.inkpulse.features.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGhnOrderMessage {
    private String orderCode;
    private String receiverName;
    private String recipientPhone; // Encrypted
    private String toAddress;
    private String toWardCode;
    private int toDistrictId;
    private String toWardName;
    private String toDistrictName;
    private String toProvinceName;
    private String paymentMethod;
    private int codAmount;
    private int totalWeight;
    private int totalLength;
    private int totalWidth;
    private int totalHeight;
    private int insuranceValue;
    private List<OrderItemInfo> items;
    private String userEmail; // Encrypted
    private String userName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo {
        private String name;
        private String code;
        private int quantity;
        private int price;
        private int weight;
        private int length;
        private int width;
        private int height;
    }
}
