package com.inkpulse.features.book.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.book.PublicBookEditionDetailResponse;
import java.util.UUID;

public record GetPublicBookEditionDetailQuery(UUID editionId) implements Query<PublicBookEditionDetailResponse> {
}
