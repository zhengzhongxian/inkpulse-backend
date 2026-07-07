package com.inkpulse.features.badge.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.badge.BadgeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetBadgeByIdQuery implements Query<BadgeResponse> {
    private UUID id;
}
