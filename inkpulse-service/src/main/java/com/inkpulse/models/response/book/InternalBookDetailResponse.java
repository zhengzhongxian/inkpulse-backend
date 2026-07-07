package com.inkpulse.models.response.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalBookDetailResponse {
    private UUID id;
    private String title;
    private String introduce;
    private String description;
    private String thumbnailUrl;
    private boolean active;
    private UUID badgeId;
    private String badgeText;
    private String badgeTextColor;
    private String badgeBgColor;
    private List<UUID> categoryIds;
    private List<UUID> authorIds;
    private List<AuthorDto> authors;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private List<InternalBookEditionShortResponse> editions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDto {
        private UUID id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternalBookEditionShortResponse {
        private UUID id;
        private String isbn;
        private BigDecimal price;
        private BigDecimal oldPrice;
        private String priceDisplay;
        private String oldPriceDisplay;
        private int editionNumber;
        private int soldCount;
        private int stockQuantity;
    }
}
