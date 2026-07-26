package com.inkpulse.features.banner.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.banner.BannerResponse;
import lombok.Value;

import java.util.UUID;

@Value
public class GetBannerDetailQuery implements Query<BannerResponse> {
    UUID bannerId;
}
