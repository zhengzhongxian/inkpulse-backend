package com.inkpulse.models.response.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {
    private UUID id;
    private String title;
    private String introduce;
    private String thumbnailUrl;

    // Badge info
    private String badgeText;
    private String badgeTextColor;
    private String badgeBgColor;

    // Price info (computed from editions)
    private BigDecimal minPrice;
    private String priceDisplay; // e.g. "chỉ từ 350.000đ"
    private String wasPriceDisplay; // e.g. "420.000đ"

    // Authors list
    private List<String> authors;

    // Total stock quantity
    private Integer totalStock;

    // Audit logs
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    // Collapsed other versions/editions
    private List<BookEditionResponse> otherVersions;

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
