package com.inkpulse.features.book.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Book;
import com.inkpulse.entities.BookAuthor;
import com.inkpulse.entities.Category;
import com.inkpulse.features.book.dto.BookEditionResponse;
import com.inkpulse.features.book.dto.InternalBookDetailResponse;
import com.inkpulse.features.book.queries.GetInternalBookDetailQuery;
import com.inkpulse.repositories.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetInternalBookDetailQueryHandler implements Query.QueryHandler<GetInternalBookDetailQuery, InternalBookDetailResponse> {

    private final BookRepository bookRepository;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public InternalBookDetailResponse handle(GetInternalBookDetailQuery query) {
        log.info("Handling GetInternalBookDetailQuery directly from DB for ID: {}", query.bookId());

        Book book = bookRepository.findById(query.bookId())
                .orElseThrow(() -> new BusinessValidationException(BookMessageConstants.BOOK_NOT_FOUND, BookMessageConstants.CODE_BOOK_NOT_FOUND));

        List<UUID> categoryIds = new ArrayList<>();
        if (book.getCategories() != null) {
            categoryIds = book.getCategories().stream()
                    .map(Category::getId)
                    .toList();
        }

        List<InternalBookDetailResponse.AuthorDto> authors = new ArrayList<>();
        List<UUID> authorIds = new ArrayList<>();
        if (book.getBookAuthors() != null) {
            authorIds = book.getBookAuthors().stream()
                    .filter(ba -> ba.getAuthor() != null)
                    .map(ba -> ba.getAuthor().getId())
                    .toList();

            authors = book.getBookAuthors().stream()
                    .filter(ba -> ba.getAuthor() != null)
                    .map(ba -> InternalBookDetailResponse.AuthorDto.builder()
                            .id(ba.getAuthor().getId())
                            .name(ba.getAuthor().getName())
                            .build())
                    .toList();
        }

        List<InternalBookDetailResponse.InternalBookEditionShortResponse> editions = new ArrayList<>();
        if (book.getEditions() != null) {
            editions = book.getEditions().stream()
                    .map(ed -> InternalBookDetailResponse.InternalBookEditionShortResponse.builder()
                            .id(ed.getId())
                            .isbn(ed.getIsbn())
                            .price(ed.getPrice())
                            .oldPrice(ed.getOldPrice())
                            .priceDisplay(BookEditionResponse.formatVnd(ed.getPrice()))
                            .oldPriceDisplay(BookEditionResponse.formatVnd(ed.getOldPrice()))
                            .editionNumber(ed.getEditionNumber())
                            .soldCount(ed.getSoldCount())
                            .stockQuantity(ed.getStockQuantity())
                            .build())
                    .toList();
        }

        return InternalBookDetailResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .introduce(book.getIntroduce())
                .description(book.getDescription())
                .thumbnailUrl(UrlHelper.buildAbsoluteUrl(publicUrl, book.getThumbnailUrl(), useSsl))
                .active(book.isActive())
                .badgeId(book.getBadge() != null ? book.getBadge().getId() : null)
                .badgeText(book.getBadge() != null ? book.getBadge().getText() : null)
                .badgeTextColor(book.getBadge() != null ? book.getBadge().getTextColor() : null)
                .badgeBgColor(book.getBadge() != null ? book.getBadge().getBgColor() : null)
                .categoryIds(categoryIds)
                .authorIds(authorIds)
                .authors(authors)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .editions(editions)
                .build();
    }
}
