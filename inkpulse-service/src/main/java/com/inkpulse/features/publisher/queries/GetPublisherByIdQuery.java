package com.inkpulse.features.publisher.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.publisher.PublisherResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetPublisherByIdQuery implements Query<PublisherResponse> {
    private UUID id;
}
