package com.inkpulse.features.book.handlers;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.InnerHitsResult;
import co.elastic.clients.json.JsonData;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Book;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.features.book.dto.BookEditionResponse;
import com.inkpulse.features.book.dto.BookResponse;
import com.inkpulse.features.book.elastic.BookEditionDocument;
import com.inkpulse.features.book.queries.GetBooksQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.repositories.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetBooksQueryHandler implements Query.QueryHandler<GetBooksQuery, PagedList<BookResponse>> {

    private final ElasticsearchClient elasticsearchClient;
    private final BookRepository bookRepository;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.STORAGE_PDF_PUBLIC_URL + "}")
    private String pdfPublicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public PagedList<BookResponse> handle(GetBooksQuery query) {
        log.info("Handling GetBooksQuery via ELS: page={}, size={}, keyword={}, category={}",
                query.getPageNumber(), query.getPageSize(), query.getSearchKeyword(), query.getCategorySlug());

        try {
            int from = Math.max(0, (query.getPageNumber() - 1) * query.getPageSize());
            int size = query.getPageSize();

            SearchResponse<BookEditionDocument> response = elasticsearchClient.search(s -> s
                    .index("inkpulse_books")
                    .query(q -> q
                            .bool(b -> {
                                // Default filters
                                b.filter(f -> f.term(t -> t.field("is_active").value(true)));
                                b.filter(f -> f.term(t -> t.field("is_deleted").value(false)));

                                // Keyword search
                                if (query.getSearchKeyword() != null && !query.getSearchKeyword().isBlank()) {
                                    String kw = query.getSearchKeyword().trim();
                                    b.must(m -> m.multiMatch(mm -> mm
                                            .fields("book_title", "description", "introduce", "author")
                                            .query(kw)
                                    ));
                                }

                                // Category filter
                                if (query.getCategorySlug() != null && !query.getCategorySlug().isBlank()
                                        && !query.getCategorySlug().equalsIgnoreCase("all")
                                        && !query.getCategorySlug().equalsIgnoreCase("tat-ca")) {
                                    b.filter(f -> f.term(t -> t.field("category_slugs").value(query.getCategorySlug().trim())));
                                }

                                // Author filter
                                if (query.getAuthorName() != null && !query.getAuthorName().isBlank()) {
                                    b.filter(f -> f.match(m -> m.field("author").query(query.getAuthorName().trim())));
                                }

                                // Cover Type filter
                                if (query.getCoverType() != null && !query.getCoverType().isBlank()) {
                                    b.filter(f -> f.term(t -> t.field("cover_type").value(query.getCoverType().trim())));
                                }

                                // Price range filter (build dynamically to avoid passing nulls)
                                if (query.getMinPrice() != null || query.getMaxPrice() != null) {
                                    b.filter(f -> f.range(r -> r
                                            .number(n -> {
                                                n.field("price");
                                                if (query.getMinPrice() != null) {
                                                    n.gte(query.getMinPrice().doubleValue());
                                                }
                                                if (query.getMaxPrice() != null) {
                                                    n.lte(query.getMaxPrice().doubleValue());
                                                }
                                                return n;
                                            })
                                    ));
                                }

                                return b;
                            })
                    )
                    .collapse(c -> c
                            .field("book_id")
                            .innerHits(ih -> ih
                                    .name("other_versions")
                                    .size(5)
                            )
                    )
                    .sort(so -> {
                        String sortBy = query.getSortBy() != null ? query.getSortBy().trim() : "price";
                        if ("title".equalsIgnoreCase(sortBy)) {
                            sortBy = "book_title.keyword";
                        }
                        String finalSortBy = sortBy;
                        SortOrder order = "desc".equalsIgnoreCase(query.getSortDirection()) ? SortOrder.Desc : SortOrder.Asc;
                        so.field(f -> f.field(finalSortBy).order(order));
                        return so;
                    })
                    .from(from)
                    .size(size),
                    BookEditionDocument.class
            );

            List<BookResponse> list = new ArrayList<>();
            for (Hit<BookEditionDocument> hit : response.hits().hits()) {
                BookEditionDocument doc = hit.source();
                if (doc == null) continue;

                // Build BookResponse
                BookResponse bookRes = BookResponse.builder()
                        .id(UUID.fromString(doc.getBookId()))
                        .title(doc.getTitle())
                        .introduce(doc.getIntroduce())
                        .thumbnailUrl(UrlHelper.buildAbsoluteUrl(publicUrl, doc.getBookThumbnailUrl(), useSsl))
                        .badgeText(doc.getBadgeText())
                        .badgeTextColor(doc.getBadgeTextColor())
                        .badgeBgColor(doc.getBadgeBgColor())
                        .minPrice(doc.getPrice())
                        .priceDisplay("chỉ từ " + BookResponse.formatVnd(doc.getPrice()))
                        .wasPriceDisplay(doc.getOldPrice() != null ? BookResponse.formatVnd(doc.getOldPrice()) : null)
                        .authors(doc.getAuthorName() != null ? List.of(doc.getAuthorName().split(",\\s*")) : List.of())
                        .build();

                // Extract alternative versions from inner hits
                List<BookEditionResponse> otherVersions = new ArrayList<>();
                Map<String, InnerHitsResult> innerHitsMap = hit.innerHits();
                if (innerHitsMap != null && innerHitsMap.containsKey("other_versions")) {
                    InnerHitsResult innerHitsResult = innerHitsMap.get("other_versions");
                    if (innerHitsResult != null && innerHitsResult.hits() != null) {
                        for (Hit<JsonData> vHit : innerHitsResult.hits().hits()) {
                            JsonData sourceJson = vHit.source();
                            if (sourceJson == null) continue;

                            BookEditionDocument vDoc = sourceJson.to(BookEditionDocument.class);

                            otherVersions.add(BookEditionResponse.builder()
                                    .id(UUID.fromString(vDoc.getId()))
                                    .bookId(UUID.fromString(vDoc.getBookId()))
                                    .bookTitle(vDoc.getTitle())
                                    .isbn(vDoc.getIsbn())
                                    .price(vDoc.getPrice())
                                    .oldPrice(vDoc.getOldPrice())
                                    .priceDisplay(BookEditionResponse.formatVnd(vDoc.getPrice()))
                                    .oldPriceDisplay(BookEditionResponse.formatVnd(vDoc.getOldPrice()))
                                    .stockQuantity(vDoc.getStockQuantity())
                                    .editionNumber(vDoc.getEditionNumber())
                                    .thumbnailUrl(UrlHelper.buildAbsoluteUrl(publicUrl, vDoc.getThumbnailUrl(), useSsl))
                                    .filePathPdf(vDoc.getFilePathPdf())
                                    .filePathPdfUrl(UrlHelper.buildAbsoluteUrl(pdfPublicUrl, vDoc.getFilePathPdf(), useSsl))
                                    .coverType(vDoc.getCoverType())
                                    .pageCount(vDoc.getPageCount())
                                    .publicationYear(vDoc.getPublicationYear())
                                    .dimensions(vDoc.getDimensions())
                                    .language(vDoc.getLanguage())
                                    .publisherName(vDoc.getPublisherName())
                                    .build());
                        }
                    }
                }
                bookRes.setOtherVersions(otherVersions);
                list.add(bookRes);
            }

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            return new PagedList<>(list, (int) total, query.getPageNumber(), query.getPageSize());

        } catch (Exception ex) {
            log.error("Failed to query books from Elasticsearch, falling back to PostgreSQL database", ex);
            return fallbackToDatabase(query);
        }
    }

    private PagedList<BookResponse> fallbackToDatabase(GetBooksQuery query) {
        int pageIndex = Math.max(0, query.getPageNumber() - 1);
        int size = query.getPageSize();

        Sort sort;
        if (query.getSortBy() != null && !query.getSortBy().trim().isEmpty()) {
            String cleanSort = query.getSortBy().trim();
            if ("price".equalsIgnoreCase(cleanSort)) {
                cleanSort = "be.price";
            } else if ("stock".equalsIgnoreCase(cleanSort)) {
                cleanSort = "be.stockQuantity";
            }
            Sort.Direction direction = "desc".equalsIgnoreCase(query.getSortDirection()) ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            sort = Sort.by(direction, cleanSort).and(Sort.by(Sort.Direction.ASC, "id"));
        } else {
            sort = Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.ASC, "id"));
        }

        Pageable pageable = PageRequest.of(pageIndex, size, sort);

        String categorySlug = query.getCategorySlug();
        if (categorySlug != null && (categorySlug.equalsIgnoreCase("all") || categorySlug.equalsIgnoreCase("tat-ca") || categorySlug.trim().isEmpty())) {
            categorySlug = null;
        }

        String keyword = query.getSearchKeyword();
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        Page<Book> bookPage = bookRepository.searchBooks(
                categorySlug,
                keyword,
                query.getAuthorName(),
                query.getCoverType(),
                query.getMinPrice(),
                query.getMaxPrice(),
                pageable);
        return PagedList.fromPage(bookPage, this::mapToResponse);
    }

    private BookResponse mapToResponse(Book book) {
        BookEdition minEdition = book.getEditions().stream()
                .filter(e -> e.getPrice() != null)
                .min(Comparator.comparing(BookEdition::getPrice))
                .orElse(null);

        String priceDisplay = "Liên hệ";
        String wasPriceDisplay = null;
        BigDecimal minPrice = null;

        if (minEdition != null) {
            minPrice = minEdition.getPrice();
            priceDisplay = "chỉ từ " + BookResponse.formatVnd(minPrice);
            if (minEdition.getOldPrice() != null) {
                wasPriceDisplay = BookResponse.formatVnd(minEdition.getOldPrice());
            }
        }

        List<String> authors = book.getBookAuthors().stream()
                .filter(ba -> ba.isActive() && ba.getAuthor() != null)
                .map(ba -> ba.getAuthor().getName())
                .toList();

        String badgeText = book.getBadge() != null ? book.getBadge().getText() : null;
        String badgeTextColor = book.getBadge() != null ? book.getBadge().getTextColor() : null;
        String badgeBgColor = book.getBadge() != null ? book.getBadge().getBgColor() : null;

        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .introduce(book.getIntroduce())
                .thumbnailUrl(UrlHelper.buildAbsoluteUrl(publicUrl, book.getThumbnailUrl(), useSsl))
                .badgeText(badgeText)
                .badgeTextColor(badgeTextColor)
                .badgeBgColor(badgeBgColor)
                .minPrice(minPrice)
                .priceDisplay(priceDisplay)
                .wasPriceDisplay(wasPriceDisplay)
                .authors(authors)
                .build();
    }
}
