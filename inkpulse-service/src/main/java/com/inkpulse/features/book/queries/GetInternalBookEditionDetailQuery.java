package com.inkpulse.features.book.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.features.book.dto.InternalBookEditionDetailResponse;
import java.util.UUID;

public record GetInternalBookEditionDetailQuery(UUID editionId) implements Query<InternalBookEditionDetailResponse> {
}
