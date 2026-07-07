package com.inkpulse.features.book.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Book;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Category;
import com.inkpulse.models.response.book.BookEditionResponse;
import com.inkpulse.models.response.book.BookResponse;
import com.inkpulse.features.book.queries.GetInternalBooksQuery;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetInternalBooksQueryHandler
        implements Query.QueryHandler<GetInternalBooksQuery, PagedList<BookResponse>> {

    private final BookRepository bookRepository;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.STORAGE_PDF_PUBLIC_URL + "}")
    private String pdfPublicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public PagedList<BookResponse> handle(GetInternalBooksQuery query) {
        log.info("Handling GetInternalBooksQuery via DB: page={}, size={}, search={}, active={}",
                query.getPageNumber(), query.getPageSize(), query.getSearchKeyword(), query.getActive());

        if (query.getSortBy() != null && !query.getSortBy().trim().isEmpty()) {
            String cleanSort = query.getSortBy().trim();
            if ("price".equalsIgnoreCase(cleanSort)) {
                query.setSortBy("be.price");
            } else if ("stock".equalsIgnoreCase(cleanSort)) {
                query.setSortBy("be.stockQuantity");
            }
        } else {
            query.setSortBy("updatedAt");
            query.setSortDirection("desc");
        }

        Pageable basePageable = query.toPageable();
        Pageable pageable = PageRequest.of(
                basePageable.getPageNumber(),
                basePageable.getPageSize(),
                basePageable.getSort().and(Sort.by(Sort.Direction.ASC, "id"))
        );

        String categorySlug = query.getCategorySlug();
        if (categorySlug != null && (categorySlug.equalsIgnoreCase("all") || categorySlug.equalsIgnoreCase("tat-ca")
                || categorySlug.trim().isEmpty())) {
            categorySlug = null;
        }

        String keyword = query.getSearchKeyword();
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        String authorName = query.getAuthorName();
        if (authorName != null && authorName.trim().isEmpty()) {
            authorName = null;
        }

        String coverType = query.getCoverType();
        if (coverType != null && coverType.trim().isEmpty()) {
            coverType = null;
        }

        Page<Book> bookPage = bookRepository.searchBooksInternal(
                categorySlug,
                keyword,
                authorName,
                coverType,
                query.getMinPrice(),
                query.getMaxPrice(),
                query.getActive(),
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

        int totalStock = book.getEditions().stream()
                .mapToInt(com.inkpulse.entities.BookEdition::getStockQuantity)
                .sum();

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
                .totalStock(totalStock)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
}
