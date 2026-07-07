package com.inkpulse.features.publisher.handlers;

import com.inkpulse.constants.message.PublisherMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Publisher;
import com.inkpulse.models.response.publisher.PublisherResponse;
import com.inkpulse.features.publisher.queries.GetPublisherByIdQuery;
import com.inkpulse.repositories.PublisherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPublisherByIdQueryHandler implements Query.QueryHandler<GetPublisherByIdQuery, PublisherResponse> {

    private final PublisherRepository publisherRepository;

    @Override
    @Transactional(readOnly = true)
    public PublisherResponse handle(GetPublisherByIdQuery query) {
        log.info("Handling GetPublisherByIdQuery: id={}", query.getId());

        Publisher pub = publisherRepository.findById(query.getId())
                .orElseThrow(() -> new BusinessValidationException(
                        PublisherMessageConstants.PUBLISHER_NOT_FOUND,
                        PublisherMessageConstants.CODE_PUBLISHER_NOT_FOUND
                ));

        return PublisherResponse.builder()
                .id(pub.getId())
                .name(pub.getName())
                .address(pub.getAddress())
                .build();
    }
}
