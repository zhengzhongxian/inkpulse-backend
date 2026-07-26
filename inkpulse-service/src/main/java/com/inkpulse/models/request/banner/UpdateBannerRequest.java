package com.inkpulse.models.request.banner;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBannerRequest {

    @NotBlank(message = "Tên banner quảng cáo không được để trống.")
    private String title;

    private String subtitle;

    private String imageUrl;

    private String iconUrl;

    private String linkUrl;

    private Integer displayOrder;

    private Boolean isActive;

    private ZonedDateTime startDate;

    private ZonedDateTime endDate;

    private List<UUID> editionIds;
}
