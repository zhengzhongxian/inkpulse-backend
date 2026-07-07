package com.inkpulse.features.book.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.EditionImage;
import com.inkpulse.models.response.book.BookEditionResponse;
import com.inkpulse.models.response.book.InternalBookEditionDetailResponse;
import com.inkpulse.features.book.queries.GetInternalBookEditionDetailQuery;
import com.inkpulse.repositories.BookEditionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetInternalBookEditionDetailQueryHandler implements Query.QueryHandler<GetInternalBookEditionDetailQuery, InternalBookEditionDetailResponse> {

    private final BookEditionRepository bookEditionRepository;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.STORAGE_PDF_PUBLIC_URL + "}")
    private String pdfPublicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public InternalBookEditionDetailResponse handle(GetInternalBookEditionDetailQuery query) {
        log.info("Handling GetInternalBookEditionDetailQuery directly from DB for ID: {}", query.editionId());

        BookEdition edition = bookEditionRepository.findById(query.editionId())
                .orElseThrow(() -> new BusinessValidationException(BookMessageConstants.BOOK_EDITION_NOT_FOUND, BookMessageConstants.CODE_BOOK_EDITION_NOT_FOUND));

        List<String> imageUrls = new ArrayList<>();
        if (edition.getImages() != null) {
            imageUrls = edition.getImages().stream()
                    .sorted(Comparator.comparingInt(EditionImage::getDisplayOrder))
                    .map(img -> UrlHelper.buildAbsoluteUrl(publicUrl, img.getImageUrl(), useSsl))
                    .toList();
        }

        List<InternalBookEditionDetailResponse.EditionBadgeDto> badges = new ArrayList<>();
        if (edition.getBadges() != null) {
            badges = edition.getBadges().stream()
                    .filter(eb -> eb.getBadge() != null && !eb.isDeleted())
                    .sorted(Comparator.comparingInt(eb -> eb.getDisplayOrder()))
                    .map(eb -> InternalBookEditionDetailResponse.EditionBadgeDto.builder()
                            .id(eb.getBadge().getId())
                            .text(eb.getBadge().getText())
                            .textColor(eb.getBadge().getTextColor())
                            .bgColor(eb.getBadge().getBgColor())
                            .build())
                    .toList();
        }

        return InternalBookEditionDetailResponse.builder()
                .id(edition.getId())
                .bookId(edition.getBook() != null ? edition.getBook().getId() : null)
                .isbn(edition.getIsbn())
                .price(edition.getPrice())
                .oldPrice(edition.getOldPrice())
                .priceDisplay(BookEditionResponse.formatVnd(edition.getPrice()))
                .oldPriceDisplay(BookEditionResponse.formatVnd(edition.getOldPrice()))
                .stockQuantity(edition.getStockQuantity())
                .editionNumber(edition.getEditionNumber())
                .thumbnailUrl(UrlHelper.buildAbsoluteUrl(publicUrl, edition.getThumbnailUrl(), useSsl))
                .coverType(edition.getCoverType() != null ? edition.getCoverType().name() : null)
                .pageCount(edition.getPageCount())
                .publicationYear(edition.getPublicationYear())
                .widthCm(edition.getWidthCm())
                .heightCm(edition.getHeightCm())
                .lengthCm(edition.getLengthCm())
                .weightGram(edition.getWeightGram())
                .language(edition.getLanguage())
                .filePathPdf(edition.getFilePathPdf())
                .filePathPdfUrl(UrlHelper.buildAbsoluteUrl(pdfPublicUrl, edition.getFilePathPdf(), useSsl))
                .soldCount(edition.getSoldCount())
                .ratingsCount(edition.getRatingsCount())
                .rating(edition.getRating())
                .publisherName(edition.getPublisher() != null ? edition.getPublisher().getName() : null)
                .publisherId(edition.getPublisher() != null ? edition.getPublisher().getId() : null)
                .imageUrls(imageUrls)
                .badges(badges)
                .build();
    }
}
