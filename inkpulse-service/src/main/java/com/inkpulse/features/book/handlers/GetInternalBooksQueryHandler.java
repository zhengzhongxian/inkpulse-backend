package com.inkpulse.features.book.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Book;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Category;
import com.inkpulse.features.book.dto.BookEditionResponse;
import com.inkpulse.features.book.dto.BookResponse;
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
