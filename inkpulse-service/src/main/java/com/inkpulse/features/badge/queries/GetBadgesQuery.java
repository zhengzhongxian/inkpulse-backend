package com.inkpulse.features.badge.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.badge.BadgeResponse;
import java.util.List;

public record GetBadgesQuery() implements Query<List<BadgeResponse>> {
}
