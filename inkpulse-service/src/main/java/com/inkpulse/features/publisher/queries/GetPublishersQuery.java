package com.inkpulse.features.publisher.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.features.publisher.dto.PublisherResponse;
import java.util.List;

public record GetPublishersQuery() implements Query<List<PublisherResponse>> {
}
