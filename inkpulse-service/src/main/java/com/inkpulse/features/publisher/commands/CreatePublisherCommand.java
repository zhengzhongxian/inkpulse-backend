package com.inkpulse.features.publisher.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.publisher.PublisherResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePublisherCommand implements Command<PublisherResponse> {
    private String name;
    private String address;
}
