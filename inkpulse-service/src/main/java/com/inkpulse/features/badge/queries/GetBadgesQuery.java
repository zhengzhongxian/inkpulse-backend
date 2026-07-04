package com.inkpulse.features.badge.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.features.badge.dto.BadgeResponse;
import java.util.List;

public record GetBadgesQuery() implements Query<List<BadgeResponse>> {
}
