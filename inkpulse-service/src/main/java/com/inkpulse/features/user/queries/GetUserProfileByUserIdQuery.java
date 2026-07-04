package com.inkpulse.features.user.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.features.user.dto.UserProfileCacheDto;
import java.util.UUID;

public record GetUserProfileByUserIdQuery(UUID userId) implements Query<UserProfileCacheDto> {
}
