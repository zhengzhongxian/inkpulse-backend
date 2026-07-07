package com.inkpulse.features.publisher.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Publisher;
import com.inkpulse.models.response.publisher.PublisherResponse;
import com.inkpulse.features.publisher.queries.GetPublishersQuery;
import com.inkpulse.repositories.PublisherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPublishersQueryHandler implements Query.QueryHandler<GetPublishersQuery, List<PublisherResponse>> {

    private final PublisherRepository publisherRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PublisherResponse> handle(GetPublishersQuery query) {
        log.info("Handling GetPublishersQuery to fetch all publishers");
        return publisherRepository.findAll().stream()
                .map(pub -> PublisherResponse.builder()
                        .id(pub.getId())
                        .name(pub.getName())
                        .address(pub.getAddress())
                        .build())
                .toList();
    }
}
