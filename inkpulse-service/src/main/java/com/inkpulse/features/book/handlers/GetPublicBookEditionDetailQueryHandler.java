package com.inkpulse.features.book.handlers;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.book.BookEditionResponse;
import com.inkpulse.models.response.book.PublicBookEditionDetailResponse;
import com.inkpulse.features.book.elastic.BookEditionDocument;
import com.inkpulse.features.book.queries.GetPublicBookEditionDetailQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPublicBookEditionDetailQueryHandler
        implements Query.QueryHandler<GetPublicBookEditionDetailQuery, PublicBookEditionDetailResponse> {

    private final ElasticsearchClient elasticsearchClient;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    public PublicBookEditionDetailResponse handle(GetPublicBookEditionDetailQuery query) {
        log.info("Handling GetPublicBookEditionDetailQuery for ID: {}", query.editionId());

        CacheProperties.SectionConfig section = cacheProperties.getSections()
                .get(KeyConstants.SECTION_BOOK_EDITION_DETAIL);
        if (section == null) {
            throw new IllegalStateException("Cache section '" + KeyConstants.SECTION_BOOK_EDITION_DETAIL
                    + "' is not configured in application.yml");
        }

        String cacheKey = section.getKey() + query.editionId().toString();
        Duration cacheTtl = Duration.ofMinutes(section.getTtl());

        // 1. Try Cache Aside
        try {
            PublicBookEditionDetailResponse cached = cacheService.get(cacheKey, PublicBookEditionDetailResponse.class);
            if (cached != null) {
                log.debug("BookEdition detail cache hit for ID: {}", query.editionId());
                return cached;
            }
        } catch (Exception e) {
            log.error("Failed to read BookEdition detail cache for ID: {}", query.editionId(), e);
        }

        // 2. Query Elasticsearch with fallback search
        BookEditionDocument doc = null;
        try {
            GetResponse<BookEditionDocument> getResponse = elasticsearchClient.get(g -> g
                    .index("inkpulse_books")
                    .id(query.editionId().toString()), BookEditionDocument.class);

            if (getResponse.found() && getResponse.source() != null) {
                doc = getResponse.source();
            }
        } catch (Exception ex) {
            log.warn("Direct document get failed for ID: {}. Trying fallback search. Error: {}", query.editionId(), ex.getMessage());
        }

        if (doc == null) {
            try {
                final String bookIdStr = query.editionId().toString();
                SearchResponse<BookEditionDocument> searchResponse = elasticsearchClient.search(s -> s
                        .index("inkpulse_books")
                        .query(q -> q.bool(b -> b
                                .must(QueryBuilders.term(t -> t.field("book_id").value(bookIdStr)))
                                .must(QueryBuilders.term(t -> t.field("is_active").value(true)))
                                .must(QueryBuilders.term(t -> t.field("is_deleted").value(false)))))
                        .sort(sort -> sort.field(f -> f.field("price").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)))
                        .size(1), BookEditionDocument.class);

                if (searchResponse.hits() != null && !searchResponse.hits().hits().isEmpty()) {
                    doc = searchResponse.hits().hits().get(0).source();
                }
            } catch (Exception ex) {
                log.error("Failed to query BookEdition fallback search from Elasticsearch. ID: {}", query.editionId(), ex);
            }
        }

        if (doc == null || !doc.isActive() || doc.isDeleted()) {
            throw new BusinessValidationException(BookMessageConstants.BOOK_EDITION_NOT_FOUND,
                    BookMessageConstants.CODE_BOOK_EDITION_NOT_FOUND);
        }

        // 3. Map to Response
        List<String> absoluteImageUrls = new ArrayList<>();
        if (doc.getImageUrls() != null) {
            absoluteImageUrls = doc.getImageUrls().stream()
                    .map(path -> UrlHelper.buildAbsoluteUrl(publicUrl, path, useSsl))
                    .toList();
        }

        List<PublicBookEditionDetailResponse.BadgeDto> badges = new ArrayList<>();
        if (doc.getBadges() != null) {
            badges = doc.getBadges().stream()
                    .map(b -> PublicBookEditionDetailResponse.BadgeDto.builder()
                            .text(b.getText())
                            .textColor(b.getTextColor())
                            .bgColor(b.getBgColor())
                            .shape(b.getShape())
                            .build())
                    .toList();
        }

        // Query other editions of the same book
        List<BookEditionResponse> otherVersions = fetchOtherVersions(doc.getBookId());

        String stockStatus;
        if (doc.getStockQuantity() >= 10) {
            stockStatus = "Còn hàng";
        } else if (doc.getStockQuantity() > 0) {
            stockStatus = "Chỉ còn " + doc.getStockQuantity() + " cuốn";
        } else {
            stockStatus = "Tạm hết hàng";
        }

        PublicBookEditionDetailResponse detail = PublicBookEditionDetailResponse.builder()
                .id(UUID.fromString(doc.getId()))
                .isbn(doc.getIsbn())
                .price(doc.getPrice())
                .oldPrice(doc.getOldPrice())
                .priceDisplay(BookEditionResponse.formatVnd(doc.getPrice()))
                .oldPriceDisplay(BookEditionResponse.formatVnd(doc.getOldPrice()))
                .stockQuantity(doc.getStockQuantity())
                .stockStatus(stockStatus)
                .editionNumber(doc.getEditionNumber())
                .soldCount(doc.getSoldCount())
                .thumbnailUrl(UrlHelper.buildAbsoluteUrl(publicUrl, doc.getThumbnailUrl(), useSsl))
                .coverType(doc.getCoverType())
                .pageCount(doc.getPageCount())
                .publicationYear(doc.getPublicationYear())
                .widthCm(doc.getWidthCm())
                .heightCm(doc.getHeightCm())
                .lengthCm(doc.getLengthCm())
                .weightGram(doc.getWeightGram())
                .language(doc.getLanguage())
                .publisherName(doc.getPublisherName())
                .imageUrls(absoluteImageUrls)
                .bookId(UUID.fromString(doc.getBookId()))
                .bookTitle(doc.getTitle())
                .bookThumbnailUrl(UrlHelper.buildAbsoluteUrl(publicUrl, doc.getBookThumbnailUrl(), useSsl))
                .introduce(doc.getIntroduce())
                .description(doc.getDescription())
                .authorName(doc.getAuthorName())
                .badgeText(doc.getBadgeText())
                .badgeTextColor(doc.getBadgeTextColor())
                .badgeBgColor(doc.getBadgeBgColor())
                .categorySlugs(doc.getCategorySlugs())
                .badges(badges)
                .otherVersions(otherVersions)
                .build();

        // 4. Save Cache and register Redis Sets (sadd)
        try {
            cacheService.set(cacheKey, detail, cacheTtl);

            String editionIdStr = detail.getId().toString();

            // Book Set
            if (doc.getBookId() != null) {
                String bookSetKey = cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITIONS, doc.getBookId());
                cacheService.sadd(bookSetKey, cacheTtl, editionIdStr);
            }

            // Author Sets
            if (doc.getAuthorIds() != null) {
                for (String authorId : doc.getAuthorIds()) {
                    String authorSetKey = cacheProperties.buildKey(KeyConstants.SECTION_AUTHOR_EDITIONS, authorId);
                    cacheService.sadd(authorSetKey, cacheTtl, editionIdStr);
                }
            }

            // Publisher Set
            if (doc.getPublisherId() != null) {
                String publisherSetKey = cacheProperties.buildKey(KeyConstants.SECTION_PUBLISHER_EDITIONS, doc.getPublisherId());
                cacheService.sadd(publisherSetKey, cacheTtl, editionIdStr);
            }

            // Badge Sets
            if (doc.getBadgeIds() != null) {
                for (String badgeId : doc.getBadgeIds()) {
                    String badgeSetKey = cacheProperties.buildKey(KeyConstants.SECTION_BADGE_EDITIONS, badgeId);
                    cacheService.sadd(badgeSetKey, cacheTtl, editionIdStr);
                }
            }

            // Category Sets
            if (doc.getCategorySlugs() != null) {
                for (String categorySlug : doc.getCategorySlugs()) {
                    String categorySetKey = cacheProperties.buildKey(KeyConstants.SECTION_CATEGORY_EDITIONS, categorySlug);
                    cacheService.sadd(categorySetKey, cacheTtl, editionIdStr);
                }
            }
        } catch (Exception e) {
            log.error("Failed to save BookEdition detail to cache or register Sets for ID: {}", query.editionId(), e);
        }

        return detail;
    }

    private List<BookEditionResponse> fetchOtherVersions(String bookId) {
        try {
            SearchResponse<BookEditionDocument> searchResponse = elasticsearchClient.search(s -> s
                    .index("inkpulse_books")
                    .query(q -> q.bool(b -> b
                            .must(QueryBuilders.term(t -> t.field("book_id").value(bookId)))
                            .must(QueryBuilders.term(t -> t.field("is_active").value(true)))
                            .must(QueryBuilders.term(t -> t.field("is_deleted").value(false)))))
                    .size(20), BookEditionDocument.class);

            List<BookEditionResponse> result = new ArrayList<>();
            searchResponse.hits().hits().forEach(hit -> {
                BookEditionDocument item = hit.source();
                if (item != null) {
                    BigDecimal price = item.getPrice();
                    BigDecimal oldPrice = item.getOldPrice();
                    BookEditionResponse ver = BookEditionResponse.builder()
                            .id(UUID.fromString(item.getId()))
                            .bookId(UUID.fromString(item.getBookId()))
                            .bookTitle(item.getTitle())
                            .isbn(item.getIsbn())
                            .price(price)
                            .oldPrice(oldPrice)
                            .priceDisplay(BookEditionResponse.formatVnd(price))
                            .oldPriceDisplay(BookEditionResponse.formatVnd(oldPrice))
                            .stockQuantity(item.getStockQuantity())
                            .editionNumber(item.getEditionNumber())
                            .thumbnailUrl(UrlHelper.buildAbsoluteUrl(publicUrl, item.getThumbnailUrl(), useSsl))
                            .coverType(item.getCoverType())
                            .pageCount(item.getPageCount())
                            .publicationYear(item.getPublicationYear())
                            .widthCm(item.getWidthCm())
                            .heightCm(item.getHeightCm())
                            .lengthCm(item.getLengthCm())
                            .weightGram(item.getWeightGram())
                            .language(item.getLanguage())
                            .publisherName(item.getPublisherName())
                            .build();
                    result.add(ver);
                }
            });
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch other versions for book ID: {}", bookId, e);
            return List.of();
        }
    }
}