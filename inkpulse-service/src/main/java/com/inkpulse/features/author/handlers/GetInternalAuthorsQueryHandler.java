package com.inkpulse.features.author.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Author;
import com.inkpulse.features.author.dto.AuthorResponse;
import com.inkpulse.features.author.queries.GetInternalAuthorsQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.repositories.AuthorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetInternalAuthorsQueryHandler implements Query.QueryHandler<GetInternalAuthorsQuery, PagedList<AuthorResponse>> {

    private final AuthorRepository authorRepository;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public PagedList<AuthorResponse> handle(GetInternalAuthorsQuery query) {
        log.info("Handling GetInternalAuthorsQuery via DB: page={}, size={}, search={}",
                query.getPageNumber(), query.getPageSize(), query.getSearchKeyword());

        int pageIndex = Math.max(0, query.getPageNumber() - 1);
        int size = query.getPageSize();
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by("name").ascending());

        Page<Author> page;
        if (query.getSearchKeyword() != null && !query.getSearchKeyword().isBlank()) {
            page = authorRepository.findByNameContainingIgnoreCase(query.getSearchKeyword().trim(), pageable);
        } else {
            page = authorRepository.findAll(pageable);
        }

        return PagedList.fromPage(page, this::mapToResponse);
    }

    private AuthorResponse mapToResponse(Author author) {
        return AuthorResponse.builder()
                .id(author.getId())
                .name(author.getName())
                .biography(author.getBiography())
                .avatarUrl(UrlHelper.buildAbsoluteUrl(publicUrl, author.getAvatar(), useSsl))
                .build();
    }
}
