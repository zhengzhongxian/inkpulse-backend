package com.inkpulse.features.book.dto;

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
public class SyncBookEditionMessage {
    private UUID id;
    private UUID bookId;
    private String title;
    private String introduce;
    private String description;
    private String bookThumbnailUrl;
    private String isbn;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private Integer stockQuantity;
    private Integer editionNumber;
    private String thumbnailUrl;
    private String filePathPdf;
    private String coverType;
    private Integer pageCount;
    private Integer publicationYear;
    private String dimensions;
    private String language;
    private String publisherName;
    private String authorName;
    private String badgeText;
    private String badgeTextColor;
    private String badgeBgColor;
    private boolean active;
    private boolean deleted;
    private List<String> categorySlugs;
    private List<String> imageUrls;
    private List<BadgeInfo> badges;
    private UUID publisherId;
    private List<UUID> authorIds;
    private List<UUID> badgeIds;
    private int soldCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BadgeInfo {
        private String text;
        private String textColor;
        private String bgColor;
        private String shape;
    }
}
