package com.inkpulse.features.publisher.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Publisher;
import com.inkpulse.features.publisher.dto.PublisherResponse;
import com.inkpulse.features.publisher.queries.GetPagedPublishersQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.repositories.PublisherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPagedPublishersQueryHandler implements Query.QueryHandler<GetPagedPublishersQuery, PagedList<PublisherResponse>> {

    private final PublisherRepository publisherRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedList<PublisherResponse> handle(GetPagedPublishersQuery query) {
        log.info("Handling GetPagedPublishersQuery: page={}, size={}, search={}",
                query.getPageNumber(), query.getPageSize(), query.getSearchKeyword());

        int pageIndex = Math.max(0, query.getPageNumber() - 1);
        int size = query.getPageSize();
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by("name").ascending());

        Page<Publisher> page;
        if (query.getSearchKeyword() != null && !query.getSearchKeyword().isBlank()) {
            page = publisherRepository.findByNameContainingIgnoreCase(query.getSearchKeyword().trim(), pageable);
        } else {
            page = publisherRepository.findAll(pageable);
        }

        return PagedList.fromPage(page, pub -> PublisherResponse.builder()
                .id(pub.getId())
                .name(pub.getName())
                .address(pub.getAddress())
                .build());
    }
}
