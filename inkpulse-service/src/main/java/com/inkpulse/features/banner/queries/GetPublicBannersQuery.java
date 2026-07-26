package com.inkpulse.features.banner.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.banner.BannerResponse;
import lombok.Value;

import java.util.List;

@Value
public class GetPublicBannersQuery implements Query<List<BannerResponse>> {
}
