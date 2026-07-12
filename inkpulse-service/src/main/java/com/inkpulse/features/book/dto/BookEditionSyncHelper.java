package com.inkpulse.features.book.dto;

import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.EditionImage;
import com.inkpulse.entities.Category;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BookEditionSyncHelper {

    public SyncBookEditionMessage buildSyncMessage(BookEdition edition) {
        if (edition == null || edition.getBook() == null) {
            return null;
        }

        var book = edition.getBook();

        String authorNameJoined = "";
        List<UUID> authorIds = new ArrayList<>();
        if (book.getBookAuthors() != null) {
            authorNameJoined = book.getBookAuthors().stream()
                    .filter(ba -> ba.isActive() && ba.getAuthor() != null)
                    .map(ba -> ba.getAuthor().getName())
                    .collect(Collectors.joining(", "));

            authorIds = book.getBookAuthors().stream()
                    .filter(ba -> ba.isActive() && ba.getAuthor() != null)
                    .map(ba -> ba.getAuthor().getId())
                    .toList();
        }

        List<String> categorySlugs = new ArrayList<>();
        if (book.getCategories() != null) {
            categorySlugs = book.getCategories().stream()
                    .map(Category::getSlug)
                    .toList();
        }

        String badgeText = book.getBadge() != null ? book.getBadge().getText() : null;
        String badgeTextColor = book.getBadge() != null ? book.getBadge().getTextColor() : null;
        String badgeBgColor = book.getBadge() != null ? book.getBadge().getBgColor() : null;

        List<SyncBookEditionMessage.BadgeInfo> editionBadges = new ArrayList<>();
        List<UUID> badgeIds = new ArrayList<>();
        if (edition.getBadges() != null) {
            editionBadges = edition.getBadges().stream()
                    .filter(eb -> eb.getBadge() != null && !eb.isDeleted())
                    .map(eb -> SyncBookEditionMessage.BadgeInfo.builder()
                            .text(eb.getBadge().getText())
                            .textColor(eb.getBadge().getTextColor())
                            .bgColor(eb.getBadge().getBgColor())
                            .shape(eb.getBadge().getShape())
                            .build())
                    .toList();

            badgeIds = edition.getBadges().stream()
                    .filter(eb -> eb.getBadge() != null && !eb.isDeleted())
                    .map(eb -> eb.getBadge().getId())
                    .toList();
        }

        List<String> imageRelativePaths = new ArrayList<>();
        if (edition.getImages() != null) {
            imageRelativePaths = edition.getImages().stream()
                    .map(EditionImage::getImageUrl)
                    .collect(Collectors.toList());
        }

        return SyncBookEditionMessage.builder()
                .id(edition.getId())
                .bookId(book.getId())
                .title(book.getTitle())
                .introduce(book.getIntroduce())
                .description(book.getDescription())
                .bookThumbnailUrl(book.getThumbnailUrl())
                .isbn(edition.getIsbn())
                .price(edition.getPrice())
                .oldPrice(edition.getOldPrice())
                .stockQuantity(edition.getStockQuantity())
                .editionNumber(edition.getEditionNumber())
                .thumbnailUrl(edition.getThumbnailUrl())
                .filePathPdf(edition.getFilePathPdf())
                .coverType(edition.getCoverType() != null ? edition.getCoverType().name() : null)
                .pageCount(edition.getPageCount())
                .publicationYear(edition.getPublicationYear())
                .widthCm(edition.getWidthCm())
                .heightCm(edition.getHeightCm())
                .lengthCm(edition.getLengthCm())
                .weightGram(edition.getWeightGram())
                .language(edition.getLanguage())
                .publisherName(edition.getPublisher() != null ? edition.getPublisher().getName() : null)
                .authorName(authorNameJoined)
                .badgeText(badgeText)
                .badgeTextColor(badgeTextColor)
                .badgeBgColor(badgeBgColor)
                .active(book.isActive())
                .deleted(book.isDeleted())
                .categorySlugs(categorySlugs)
                .imageUrls(imageRelativePaths)
                .badges(editionBadges)
                .publisherId(edition.getPublisher() != null ? edition.getPublisher().getId() : null)
                .authorIds(authorIds)
                .badgeIds(badgeIds)
                .soldCount(edition.getSoldCount())
                .build();
    }
}
