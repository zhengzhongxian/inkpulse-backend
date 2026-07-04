package com.inkpulse.features.author.dto;

import com.inkpulse.features.book.dto.BookResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDetailResponse {
    private UUID id;
    private String name;
    private String avatarUrl;
    private String biography;
    private List<BookResponse> books;
}
