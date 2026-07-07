package com.inkpulse.features.book.commands;

import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.book.BookEditionResponse;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookEditionCommand implements Command<BookEditionResponse> {
    @NotNull(message = BookMessageConstants.Validate.BOOK_ID_EMPTY)
    private UUID bookId;

    @NotBlank(message = BookMessageConstants.Validate.ISBN_EMPTY)
    @Size(max = 50, message = BookMessageConstants.Validate.ISBN_TOO_LONG)
    private String isbn;

    @NotNull(message = BookMessageConstants.Validate.PRICE_EMPTY)
    @DecimalMin(value = "0.01", message = BookMessageConstants.Validate.PRICE_INVALID)
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = BookMessageConstants.Validate.OLD_PRICE_INVALID)
    private BigDecimal oldPrice;

    @Min(value = 0, message = BookMessageConstants.Validate.STOCK_INVALID)
    private int stockQuantity;

    @Min(value = 1, message = BookMessageConstants.Validate.EDITION_NUMBER_INVALID)
    private int editionNumber;

    @Size(max = 50, message = BookMessageConstants.Validate.COVER_TYPE_TOO_LONG)
    private String coverType;

    @Min(value = 1, message = BookMessageConstants.Validate.PAGE_COUNT_INVALID)
    private Integer pageCount;

    @Min(value = 1000, message = BookMessageConstants.Validate.PUBLICATION_YEAR_INVALID)
    @Max(value = 9999, message = BookMessageConstants.Validate.PUBLICATION_YEAR_INVALID)
    private Integer publicationYear;

    @Min(value = 0, message = "Cân nặng phải từ 0 trở lên")
    private int weightGram;

    @Min(value = 0, message = "Chiều rộng phải từ 0 trở lên")
    private int widthCm;

    @Min(value = 0, message = "Chiều cao phải từ 0 trở lên")
    private int heightCm;

    @Min(value = 0, message = "Chiều dài phải từ 0 trở lên")
    private int lengthCm;

    @Size(max = 50, message = BookMessageConstants.Validate.LANGUAGE_TOO_LONG)
    private String language;

    private UUID publisherId;
    private List<UUID> badgeIds;

    // Upload Files
    private UploadFileModel coverFile;
    private UploadFileModel pdfFile;
    private List<UploadFileModel> additionalImages;
}
