package com.inkpulse.features.author.handlers;

import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Author;
import com.inkpulse.entities.Book;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Category;
import com.inkpulse.features.author.dto.AuthorDetailResponse;
import com.inkpulse.features.author.queries.GetAuthorDetailQuery;
import com.inkpulse.features.book.dto.BookEditionResponse;
import com.inkpulse.features.book.dto.BookResponse;
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
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetAuthorDetailQueryHandler implements Query.QueryHandler<GetAuthorDetailQuery, AuthorDetailResponse> {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.STORAGE_PDF_PUBLIC_URL + "}")
    private String pdfPublicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Value("${" + KeyConstants.CATEGORY_LOCK_RETRY_TIMEOUT + ":5}")
    private int retryTimeoutSeconds;

    @Value("${" + KeyConstants.CATEGORY_LOCK_RETRY_INTERVAL + ":100}")
    private int retryIntervalMs;

    @Override
    @Transactional(readOnly = true)
    public AuthorDetailResponse handle(GetAuthorDetailQuery query) {
        log.info("Handling GetAuthorDetailQuery for ID: {}", query.getAuthorId());

        CacheProperties.SectionConfig section = cacheProperties.getSections().get(KeyConstants.SECTION_AUTHOR_DETAIL);
        if (section == null) {
            throw new IllegalStateException("Cache section '" + KeyConstants.SECTION_AUTHOR_DETAIL + "' is not configured in application.yml");
        }

        String cacheKey = section.getKey() + query.getAuthorId().toString();
        String lockKey = "lock:" + cacheKey;
        Duration cacheTtl = Duration.ofMinutes(section.getTtl());
        Duration lockTtl = Duration.ofSeconds(10);

        AuthorDetailResponse cached = cacheService.get(cacheKey, AuthorDetailResponse.class);
        if (cached != null) {
            log.debug("Author detail cache hit for ID: {}", query.getAuthorId());
            return cached;
        }

        log.debug("Author detail cache miss. Attempting to acquire lock for ID: {}", query.getAuthorId());
        String lockValue = UUID.randomUUID().toString();

        if (cacheService.acquireLock(lockKey, lockValue, lockTtl, true, Duration.ofSeconds(retryTimeoutSeconds),
                Duration.ofMillis(retryIntervalMs))) {
            log.debug("Acquired lock for loading author detail for ID: {}", query.getAuthorId());
            try {
                cached = cacheService.get(cacheKey, AuthorDetailResponse.class);
                if (cached != null) {
                    log.debug("Author detail cache hit on double-check for ID: {}", query.getAuthorId());
                    return cached;
                }

                AuthorDetailResponse detail = loadFromDb(query.getAuthorId());
                cacheService.set(cacheKey, detail, cacheTtl);
                log.debug("Author detail loaded from DB and saved to cache for ID: {}", query.getAuthorId());
                return detail;

            } catch (Exception e) {
                log.error("Error occurred while loading author detail from DB for ID: {}", query.getAuthorId(), e);
                throw e;
            } finally {
                boolean released = cacheService.releaseLock(lockKey, lockValue);
                log.debug("Released author detail lock: {}", released);
            }
        } else {
            log.debug("Failed to acquire lock. Falling back to direct database retrieval for ID: {}", query.getAuthorId());
            return loadFromDb(query.getAuthorId());
        }
    }

    private AuthorDetailResponse loadFromDb(UUID authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tác giả"));

        List<Book> books = bookRepository.findBooksByAuthorId(authorId);
        List<BookResponse> bookResponses = books.stream()
                .filter(Book::isActive)
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
                        .dimensions(edition.getDimensions())
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
