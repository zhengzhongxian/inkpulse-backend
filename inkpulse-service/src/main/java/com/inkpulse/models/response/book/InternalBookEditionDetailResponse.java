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
public class InternalBookEditionDetailResponse {
    private UUID id;
    private UUID bookId;
    private String isbn;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private String priceDisplay;
    private String oldPriceDisplay;
    private int stockQuantity;
    private int editionNumber;
    private String thumbnailUrl;
    private String coverType;
    private Integer pageCount;
    private Integer publicationYear;
    private int weightGram;
    private int widthCm;
    private int heightCm;
    private int lengthCm;
    private String language;
    private String filePathPdf;
    private String filePathPdfUrl;
    private int soldCount;
    private int ratingsCount;
    private BigDecimal rating;
    private String publisherName;
    private UUID publisherId;
    private List<String> imageUrls;
    private List<EditionBadgeDto> badges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditionBadgeDto {
        private UUID id; // Badge ID
        private String text;
        private String textColor;
        private String bgColor;
    }
}
