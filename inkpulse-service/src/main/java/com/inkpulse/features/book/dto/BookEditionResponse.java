package com.inkpulse.features.book.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookEditionResponse {
    private UUID id;
    private UUID bookId;
    private String bookTitle;
    private String isbn;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private String priceDisplay;
    private String oldPriceDisplay;
    private int stockQuantity;
    private int editionNumber;
    private String thumbnailUrl;
    private java.util.List<String> imageUrls;
    private String filePathPdf;      // Relative path
    private String filePathPdfUrl;   // Absolute URL using fake domain
    private int soldCount;
    private int ratingsCount;
    private BigDecimal rating;
    private String coverType;
    private Integer pageCount;
    private Integer publicationYear;
    private String dimensions;
    private String language;
    private String publisherName;

    public static String formatVnd(BigDecimal price) {
        if (price == null) {
            return null;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + "đ";
    }
}
