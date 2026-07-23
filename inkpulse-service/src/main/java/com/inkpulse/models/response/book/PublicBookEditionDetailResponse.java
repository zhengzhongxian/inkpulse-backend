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
public class PublicBookEditionDetailResponse {
    private UUID id;
    private String isbn;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private String priceDisplay;
    private String oldPriceDisplay;
    private int stockQuantity;
    private String stockStatus;
    private int editionNumber;
    private int soldCount;
    private String thumbnailUrl;
    private String coverType;
    private Integer pageCount;
    private Integer publicationYear;
    private int weightGram;
    private int widthCm;
    private int heightCm;
    private int lengthCm;
    private String language;
    private String publisherName;
    private List<String> imageUrls;

    // Book parent fields
    private UUID bookId;
    private String bookTitle;
    private String bookThumbnailUrl;
    private String introduce;
    private String description;
    private String authorName;
    private String badgeText;
    private String badgeTextColor;
    private String badgeBgColor;
    private List<String> categorySlugs;

    // Edition level badges
    private List<BadgeDto> badges;

    // Other editions of the same book
    private List<BookEditionResponse> otherVersions;

    private Boolean isFlashSale;
    private String flashSaleItemId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BadgeDto {
        private String text;
        private String textColor;
        private String bgColor;
        private String shape;
    }
}
