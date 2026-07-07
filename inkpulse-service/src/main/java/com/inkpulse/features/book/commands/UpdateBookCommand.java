package com.inkpulse.features.book.commands;

import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.book.BookResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookCommand implements Command<BookResponse> {
    private UUID id;

    @NotBlank(message = BookMessageConstants.Validate.TITLE_EMPTY)
    @Size(max = 255, message = BookMessageConstants.Validate.TITLE_TOO_LONG)
    private String title;

    @Size(max = 65535, message = BookMessageConstants.Validate.INTRODUCE_TOO_LONG)
    private String introduce;

    @Size(max = 65535, message = BookMessageConstants.Validate.DESCRIPTION_TOO_LONG)
    private String description;

    private Set<UUID> categoryIds;
    private Set<UUID> authorIds;
    private UUID badgeId;
    private InputStream coverFileStream;
    private String coverFileName;
    private String coverContentType;
    private long coverFileSize;
    private Boolean active;
}