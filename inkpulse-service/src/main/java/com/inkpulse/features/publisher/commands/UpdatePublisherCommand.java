package com.inkpulse.features.publisher.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.publisher.PublisherResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePublisherCommand implements Command<PublisherResponse> {
    private UUID id;
    private String name;
    private String address;
}
