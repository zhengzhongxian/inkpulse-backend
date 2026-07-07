package com.inkpulse.features.author.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Author;
import com.inkpulse.entities.Book;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Category;
import com.inkpulse.models.response.author.AuthorDetailResponse;
import com.inkpulse.features.author.queries.GetInternalAuthorDetailQuery;
import com.inkpulse.models.response.book.BookEditionResponse;
import com.inkpulse.models.response.book.BookResponse;
import com.inkpulse.repositories.AuthorRepository;
import com.inkpulse.repositories.BookRepository;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetInternalAuthorDetailQueryHandler implements Query.QueryHandler<GetInternalAuthorDetailQuery, AuthorDetailResponse> {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.STORAGE_PDF_PUBLIC_URL + "}")
    private String pdfPublicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public AuthorDetailResponse handle(GetInternalAuthorDetailQuery query) {
        log.info("Handling GetInternalAuthorDetailQuery directly from DB for ID: {}", query.getAuthorId());

        Author author = authorRepository.findById(query.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tác giả"));

        List<Book> books = bookRepository.findBooksByAuthorId(query.getAuthorId());
        // For internal view, list all books including inactive ones
        List<BookResponse> bookResponses = books.stream()
                .map(this::mapToBookResponse)
                .toList();

        return AuthorDetailResponse.builder()
                .id(author.getId())
                .name(author.getName())
                .avatarUrl(UrlHelper.buildAbsoluteUrl(publicUrl, author.getAvatar(), useSsl))
                .biography(author.getBiography())
                .books(bookResponses)
                .build();
    }

    private BookResponse mapToBookResponse(Book book) {
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

        List<BookEditionResponse> otherVersions = book.getEditions().stream()
                .map(edition -> BookEditionResponse.builder()
                        .id(edition.getId())
                        .bookId(book.getId())
                        .bookTitle(book.getTitle())
                        .isbn(edition.getIsbn())
                        .price(edition.getPrice())
                        .oldPrice(edition.getOldPrice())
                        .priceDisplay(BookEditionResponse.formatVnd(edition.getPrice()))
                        .oldPriceDisplay(BookEditionResponse.formatVnd(edition.getOldPrice()))
                        .stockQuantity(edition.getStockQuantity())
                        .editionNumber(edition.getEditionNumber())
                        .thumbnailUrl(UrlHelper.buildAbsoluteUrl(publicUrl, edition.getThumbnailUrl(), useSsl))
                        .filePathPdf(edition.getFilePathPdf())
                        .filePathPdfUrl(UrlHelper.buildAbsoluteUrl(pdfPublicUrl, edition.getFilePathPdf(), useSsl))
                        .coverType(edition.getCoverType() != null ? edition.getCoverType().name() : null)
                        .pageCount(edition.getPageCount())
                        .publicationYear(edition.getPublicationYear())
                        .widthCm(edition.getWidthCm())
                        .heightCm(edition.getHeightCm())
                        .lengthCm(edition.getLengthCm())
                        .weightGram(edition.getWeightGram())
                        .language(edition.getLanguage())
                        .publisherName(edition.getPublisher() != null ? edition.getPublisher().getName() : null)
                        .build())
                .toList();

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
                .otherVersions(otherVersions)
                .build();
    }
}
