package com.inkpulse.models.response.banner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponse {

    private UUID bannerId;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String iconUrl;
    private String linkUrl;
    private Integer displayOrder;
    private Boolean isActive;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BannerEditionItemResponse> editions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannerEditionItemResponse {
        private UUID editionId;
        private UUID bookId;
        private String bookTitle;
        private String isbn;
        private String price;
        private String oldPrice;
        private String coverUrl;
        private Integer displayOrder;
    }
}
