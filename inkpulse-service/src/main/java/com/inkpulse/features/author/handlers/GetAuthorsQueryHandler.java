package com.inkpulse.features.author.handlers;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Author;
import com.inkpulse.features.author.dto.AuthorResponse;
import com.inkpulse.features.author.elastic.AuthorDocument;
import com.inkpulse.features.author.queries.GetAuthorsQuery;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetAuthorsQueryHandler implements Query.QueryHandler<GetAuthorsQuery, PagedList<AuthorResponse>> {

    private final ElasticsearchClient elasticsearchClient;
    private final AuthorRepository authorRepository;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public PagedList<AuthorResponse> handle(GetAuthorsQuery query) {
        log.info("Handling GetAuthorsQuery via ELS: page={}, size={}, search={}",
                query.getPageNumber(), query.getPageSize(), query.getSearchKeyword());

        try {
            int from = Math.max(0, (query.getPageNumber() - 1) * query.getPageSize());
            int size = query.getPageSize();

            SearchResponse<AuthorDocument> response = elasticsearchClient.search(s -> s
                    .index("inkpulse_authors")
                    .query(q -> q
                            .bool(b -> {
                                // Exclude soft-deleted
                                b.filter(f -> f.term(t -> t.field("is_deleted").value(false)));

                                // Search keyword match
                                if (query.getSearchKeyword() != null && !query.getSearchKeyword().isBlank()) {
                                    String kw = query.getSearchKeyword().trim();
                                    b.must(m -> m.multiMatch(mm -> mm
                                            .fields("name", "biography")
                                            .query(kw)
                                    ));
                                }
                                return b;
                            })
                    )
                    .from(from)
                    .size(size),
                    AuthorDocument.class
            );

            List<AuthorResponse> list = new ArrayList<>();
            for (Hit<AuthorDocument> hit : response.hits().hits()) {
                AuthorDocument doc = hit.source();
                if (doc == null) continue;

                list.add(AuthorResponse.builder()
                        .id(UUID.fromString(doc.getId()))
                        .name(doc.getName())
                        .biography(doc.getBiography())
                        .avatarUrl(UrlHelper.buildAbsoluteUrl(publicUrl, doc.getAvatarUrl(), useSsl))
                        .build());
            }

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            return new PagedList<>(list, (int) total, query.getPageNumber(), query.getPageSize());

        } catch (Exception ex) {
            log.error("Failed to query authors from Elasticsearch, falling back to PostgreSQL", ex);
            return fallbackToDatabase(query);
        }
    }

    private PagedList<AuthorResponse> fallbackToDatabase(GetAuthorsQuery query) {
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
