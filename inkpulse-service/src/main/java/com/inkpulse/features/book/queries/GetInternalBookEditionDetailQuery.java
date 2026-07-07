package com.inkpulse.features.book.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.book.InternalBookEditionDetailResponse;
import java.util.UUID;

public record GetInternalBookEditionDetailQuery(UUID editionId) implements Query<InternalBookEditionDetailResponse> {
}
