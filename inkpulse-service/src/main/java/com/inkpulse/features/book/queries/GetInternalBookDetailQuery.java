package com.inkpulse.features.book.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.features.book.dto.InternalBookDetailResponse;
import java.util.UUID;

public record GetInternalBookDetailQuery(UUID bookId) implements Query<InternalBookDetailResponse> {
}
